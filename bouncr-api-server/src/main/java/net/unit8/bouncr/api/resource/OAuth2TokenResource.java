package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.api.service.OidcClaimMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OAuth2RefreshToken;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.sign.RsaJwtSigner;
import net.unit8.bouncr.util.KeyEncryptor;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.ACCESS_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;

/**
 * OAuth2 Token endpoint (Bouncr as OIDC IdP).
 * POST /oauth2/token (application/x-www-form-urlencoded)
 *
 * Supports grant_type: authorization_code, refresh_token, client_credentials.
 */
@AllowedMethods("POST")
public class OAuth2TokenResource {
    private static final Logger LOG = LoggerFactory.getLogger(OAuth2TokenResource.class);

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

        // Authenticate client (required for all grant types)
        OAuth2ClientAuthenticator authenticator = new OAuth2ClientAuthenticator(config);
        boolean basicAuthAttempted = authenticator.hasBasicAuth(request);
        OAuth2ClientAuthenticator.AuthResult authResult = authenticator.authenticate(params, request, dsl);
        if (authResult == null) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed", basicAuthAttempted);
        }
        OidcApplication app = authResult.app();
        String clientId = app.clientId();

        return switch (grantType) {
            case "authorization_code" -> handleAuthorizationCode(params, app, clientId, dsl);
            case "refresh_token" -> handleRefreshToken(params, app, clientId, dsl);
            case "client_credentials" -> handleClientCredentials(params, app, clientId, dsl);
            case null -> tokenError(OAuth2Error.INVALID_REQUEST, "grant_type is required");
            default -> tokenError(OAuth2Error.UNSUPPORTED_GRANT_TYPE, "Unsupported grant_type: " + grantType);
        };
    }

    // ==================== Authorization Code Grant ====================

    private ApiResponse handleAuthorizationCode(Parameters params, OidcApplication app, String clientId, DSLContext dsl) {
        String code = params.get("code");
        if (code == null) {
            return tokenError(OAuth2Error.INVALID_REQUEST, "code is required");
        }

        Serializable stored = storeProvider.getStore(AUTHORIZATION_CODE).read(code);
        if (stored == null) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code is invalid or expired");
        }
        storeProvider.getStore(AUTHORIZATION_CODE).delete(code);

        if (!(stored instanceof AuthorizationCode authCode)) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code data is corrupt");
        }

        long now = config.getClock().instant().getEpochSecond();
        if (now - authCode.createdAt() > config.getAuthorizationCodeExpires()) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code has expired");
        }
        if (!clientId.equals(authCode.clientId())) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Code was not issued to this client");
        }

        String redirectUri = params.get("redirect_uri");
        if (!Objects.equals(redirectUri, authCode.redirectUri())) {
            return tokenError(OAuth2Error.INVALID_GRANT, "redirect_uri does not match");
        }

        if (authCode.codeChallenge() != null) {
            String codeVerifier = params.get("code_verifier");
            if (codeVerifier == null) {
                return tokenError(OAuth2Error.INVALID_GRANT, "code_verifier is required");
            }
            if (!verifyPkce(codeVerifier, authCode.codeChallenge())) {
                return tokenError(OAuth2Error.INVALID_GRANT, "PKCE verification failed");
            }
        }

        // Build tokens
        byte[] privateKeyBytes = decryptPrivateKey(app);
        try {
            String issuer = issuer(clientId);
            String kid = RsaJwtSigner.deriveKid(app.publicKey());

            String accessToken = signAccessToken(issuer, authCode.userAccount(), clientId, authCode.scope(), kid, privateKeyBytes, now);

            Map<String, Object> idClaims = new LinkedHashMap<>();
            idClaims.put("iss", issuer);
            idClaims.put("sub", authCode.userAccount());
            idClaims.put("aud", clientId);
            idClaims.put("exp", now + config.getIdTokenExpires());
            idClaims.put("iat", now);
            if (authCode.nonce() != null) {
                idClaims.put("nonce", authCode.nonce());
            }
            OidcClaimMapper.addUserClaims(idClaims, authCode.userId(), authCode.scope(), dsl);
            String idToken = RsaJwtSigner.sign(idClaims, privateKeyBytes, kid);

            String refreshToken = UUID.randomUUID().toString();
            OAuth2RefreshToken refreshData = new OAuth2RefreshToken(
                    clientId, authCode.userId(), authCode.userAccount(), authCode.scope(), now);
            storeProvider.getStore(ACCESS_TOKEN).write(refreshToken, refreshData);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", config.getAccessTokenExpires());
            response.put("refresh_token", refreshToken);
            response.put("id_token", idToken);
            response.put("scope", authCode.scope());

            return tokenResponse(response);
        } finally {
            Arrays.fill(privateKeyBytes, (byte) 0);
        }
    }

    // ==================== Refresh Token Grant ====================

    private ApiResponse handleRefreshToken(Parameters params, OidcApplication app, String clientId, DSLContext dsl) {
        String refreshToken = params.get("refresh_token");
        if (refreshToken == null) {
            return tokenError(OAuth2Error.INVALID_REQUEST, "refresh_token is required");
        }

        Serializable stored = storeProvider.getStore(ACCESS_TOKEN).read(refreshToken);
        if (!(stored instanceof OAuth2RefreshToken refreshData)) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Refresh token is invalid or expired");
        }

        if (!clientId.equals(refreshData.clientId())) {
            return tokenError(OAuth2Error.INVALID_GRANT, "Refresh token was not issued to this client");
        }

        // Scope: use original scope or restrict via scope parameter (RFC 6749 §6)
        String requestedScope = params.get("scope");
        String scope;
        if (requestedScope != null) {
            Set<String> originalScopes = new HashSet<>(
                    Arrays.asList(refreshData.scope().split("\\s+")));
            Set<String> requested = new HashSet<>(
                    Arrays.asList(requestedScope.split("\\s+")));
            if (!originalScopes.containsAll(requested)) {
                return tokenError(OAuth2Error.INVALID_SCOPE, "Requested scope exceeds originally granted scope");
            }
            scope = requestedScope;
        } else {
            scope = refreshData.scope();
        }

        // Issue new tokens first, then delete old refresh token
        // (prevents token loss if signing/storage fails)
        long now = config.getClock().instant().getEpochSecond();
        byte[] privateKeyBytes = decryptPrivateKey(app);
        try {
            String issuer = issuer(clientId);
            String kid = RsaJwtSigner.deriveKid(app.publicKey());

            String accessToken = signAccessToken(issuer, refreshData.userAccount(), clientId, scope, kid, privateKeyBytes, now);

            String newRefreshToken = UUID.randomUUID().toString();
            OAuth2RefreshToken newRefreshData = new OAuth2RefreshToken(
                    clientId, refreshData.userId(), refreshData.userAccount(), scope, now);
            storeProvider.getStore(ACCESS_TOKEN).write(newRefreshToken, newRefreshData);

            // Delete old refresh token after new one is safely stored (rotation)
            storeProvider.getStore(ACCESS_TOKEN).delete(refreshToken);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", config.getAccessTokenExpires());
            response.put("refresh_token", newRefreshToken);
            response.put("scope", scope);

            return tokenResponse(response);
        } finally {
            Arrays.fill(privateKeyBytes, (byte) 0);
        }
    }

    // ==================== Client Credentials Grant ====================

    private ApiResponse handleClientCredentials(Parameters params, OidcApplication app, String clientId, DSLContext dsl) {
        // scope — validate against client's registered permissions
        String scope = params.get("scope");
        if (scope == null) {
            scope = "openid";
        }
        if (app.permissions() != null && !app.permissions().isEmpty()) {
            Set<String> allowedScopes = new HashSet<>();
            allowedScopes.add("openid");
            app.permissions().forEach(p -> allowedScopes.add(p.name()));
            Set<String> requested = new HashSet<>(Arrays.asList(scope.split("\\s+")));
            if (!allowedScopes.containsAll(requested)) {
                return tokenError(OAuth2Error.INVALID_SCOPE,
                        "Requested scope exceeds client's registered permissions");
            }
        }

        long now = config.getClock().instant().getEpochSecond();
        byte[] privateKeyBytes = decryptPrivateKey(app);
        try {
            String issuer = issuer(clientId);
            String kid = RsaJwtSigner.deriveKid(app.publicKey());

            String accessToken = signAccessToken(issuer, clientId, clientId, scope, kid, privateKeyBytes, now);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", config.getAccessTokenExpires());
            response.put("scope", scope);
            // No refresh_token, no id_token for client_credentials

            return tokenResponse(response);
        } finally {
            Arrays.fill(privateKeyBytes, (byte) 0);
        }
    }

    // ==================== Shared helpers ====================

    private String issuer(String clientId) {
        return config.getIssuerBaseUrl() + "/oauth2/openid/" + clientId;
    }

    private byte[] decryptPrivateKey(OidcApplication app) {
        KeyEncryptor encryptor = new KeyEncryptor(config.getKeyEncryptionKey(), config.getSecureRandom());
        return encryptor.decrypt(app.privateKey());
    }

    private String signAccessToken(String issuer, String sub, String clientId, String scope,
                                   String kid, byte[] privateKeyBytes, long now) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", sub);
        claims.put("aud", clientId);
        claims.put("exp", now + config.getAccessTokenExpires());
        claims.put("iat", now);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("scope", scope);
        claims.put("client_id", clientId);
        return RsaJwtSigner.sign(claims, privateKeyBytes, kid);
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
            LOG.warn("PKCE verification error", e);
            return false;
        }
    }

    private ApiResponse tokenResponse(Map<String, Object> body) {
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, body)
                .build();
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
                        "Cache-Control", "no-store", "Pragma", "no-cache",
                        "WWW-Authenticate", "Basic realm=\"bouncr\"")
                : Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store", "Pragma", "no-cache");
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, headers)
                .set(ApiResponse::setBody, body)
                .build();
    }
}
