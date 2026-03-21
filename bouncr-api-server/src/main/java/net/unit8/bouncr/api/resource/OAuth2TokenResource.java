package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;

/**
 * OAuth2 Token endpoint (Bouncr as OIDC IdP).
 * POST /oauth2/token (application/x-www-form-urlencoded)
 */
@AllowedMethods("POST")
public class OAuth2TokenResource {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(POST)
    public boolean doPost() {
        return true;
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(Parameters params, HttpRequest request, DSLContext dsl) {
        String grantType = params.get("grant_type");
        if (!"authorization_code".equals(grantType)) {
            return tokenError(OAuth2Error.UNSUPPORTED_GRANT_TYPE, "Only authorization_code is supported");
        }

        // 1. Authenticate client
        boolean basicAuthAttempted = hasBasicAuth(request);
        String[] clientCredentials = extractClientCredentials(params, request);
        if (clientCredentials == null) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed", basicAuthAttempted);
        }
        String clientId = clientCredentials[0];
        String clientSecret = clientCredentials[1];

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByClientId(clientId).orElse(null);
        if (app == null || !MessageDigest.isEqual(
                app.clientSecret().getBytes(StandardCharsets.UTF_8),
                clientSecret.getBytes(StandardCharsets.UTF_8))) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed", basicAuthAttempted);
        }

        // 2. Validate authorization code
        String code = params.get("code");
        if (code == null) {
            return tokenError(OAuth2Error.INVALID_REQUEST, "code is required");
        }

        Serializable stored = storeProvider.getStore(AUTHORIZATION_CODE).read(code);
        if (stored == null) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code is invalid or expired");
        }
        storeProvider.getStore(AUTHORIZATION_CODE).delete(code);

        if (!(stored instanceof AuthorizationCode)) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code data is corrupt");
        }
        AuthorizationCode authCode = (AuthorizationCode) stored;

        // 3. Server-side expiry check (defense in depth beyond store TTL)
        long now = config.getClock().instant().getEpochSecond();
        if (now - authCode.createdAt() > config.getAuthorizationCodeExpires()) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code has expired");
        }

        // 4. Validate code belongs to this client
        if (!clientId.equals(authCode.clientId())) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Code was not issued to this client");
        }

        // 5. Validate redirect_uri (required per RFC 6749 §4.1.3)
        String redirectUri = params.get("redirect_uri");
        if (!Objects.equals(redirectUri, authCode.redirectUri())) {
            return tokenError(OAuth2Error.INVALID_GRANT, "redirect_uri does not match");
        }

        // 6. PKCE verification
        if (authCode.codeChallenge() != null) {
            String codeVerifier = params.get("code_verifier");
            if (codeVerifier == null) {
                return tokenError(OAuth2Error.INVALID_GRANT, "code_verifier is required");
            }
            if (!verifyPkce(codeVerifier, authCode.codeChallenge())) {
                return tokenError(OAuth2Error.INVALID_GRANT, "PKCE verification failed");
            }
        }

        // 7. Build tokens
        String issuer = config.getIssuerBaseUrl() + "/oauth2/openid/" + clientId;
        String kid = RsaJwtSigner.deriveKid(app.publicKey());

        // access_token (RFC 9068 JWT)
        Map<String, Object> accessClaims = new LinkedHashMap<>();
        accessClaims.put("iss", issuer);
        accessClaims.put("sub", authCode.userAccount());
        accessClaims.put("aud", clientId);
        accessClaims.put("exp", now + config.getAccessTokenExpires());
        accessClaims.put("iat", now);
        accessClaims.put("jti", UUID.randomUUID().toString());
        accessClaims.put("scope", authCode.scope());
        accessClaims.put("client_id", clientId);
        String accessToken = RsaJwtSigner.sign(accessClaims, app.privateKey(), kid);

        // id_token (OIDC Core §2)
        Map<String, Object> idClaims = new LinkedHashMap<>();
        idClaims.put("iss", issuer);
        idClaims.put("sub", authCode.userAccount());
        idClaims.put("aud", clientId);
        idClaims.put("exp", now + config.getIdTokenExpires());
        idClaims.put("iat", now);
        if (authCode.nonce() != null) {
            idClaims.put("nonce", authCode.nonce());
        }
        addUserClaims(idClaims, authCode.userId(), authCode.scope(), dsl);
        String idToken = RsaJwtSigner.sign(idClaims, app.privateKey(), kid);

        // 8. Return token response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", config.getAccessTokenExpires());
        response.put("id_token", idToken);
        response.put("scope", authCode.scope());

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, response)
                .build();
    }

    private boolean hasBasicAuth(HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        return authHeader != null && authHeader.startsWith("Basic ");
    }

    private String[] extractClientCredentials(Parameters params, HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)),
                        StandardCharsets.UTF_8);
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) {
                    return parts;
                }
            } catch (IllegalArgumentException e) {
                // Invalid Base64 — fall through to POST body
            }
        }
        String clientId = params.get("client_id");
        String clientSecret = params.get("client_secret");
        if (clientId != null && clientSecret != null) {
            return new String[]{clientId, clientSecret};
        }
        return null;
    }

    private boolean verifyPkce(String codeVerifier, String codeChallenge) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    codeChallenge.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private void addUserClaims(Map<String, Object> claims, long userId, String scope, DSLContext dsl) {
        if (scope == null) return;
        Set<String> scopes = new HashSet<>(Arrays.asList(scope.split("\\s+")));
        UserRepository userRepo = new UserRepository(dsl);

        if (scopes.contains("profile") || scopes.contains("email")) {
            var profileValues = userRepo.loadProfileValues(userId);
            for (var pv : profileValues) {
                String jsonName = pv.userProfileField().jsonName();
                if (scopes.contains("email") && "email".equals(jsonName)) {
                    claims.put("email", pv.value());
                }
                if (scopes.contains("profile")) {
                    if ("name".equals(jsonName) || "family_name".equals(jsonName)
                            || "given_name".equals(jsonName) || "preferred_username".equals(jsonName)) {
                        claims.put(jsonName, pv.value());
                    }
                }
            }
        }
    }

    private ApiResponse tokenError(OAuth2Error error, String description) {
        return tokenError(error, description, false);
    }

    private ApiResponse tokenError(OAuth2Error error, String description, boolean basicAuthAttempted) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) {
            body.put("error_description", description);
        }
        Headers headers = basicAuthAttempted && error == OAuth2Error.INVALID_CLIENT
                ? Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "WWW-Authenticate", "Basic realm=\"bouncr\"")
                : Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store");
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, headers)
                .set(ApiResponse::setBody, body)
                .build();
    }
}
