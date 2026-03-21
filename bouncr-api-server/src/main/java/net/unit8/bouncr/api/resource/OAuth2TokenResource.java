package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.api.service.OidcClaimMapper;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2Error;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
        OAuth2ClientAuthenticator authenticator = new OAuth2ClientAuthenticator(config);
        OAuth2ClientAuthenticator.AuthResult authResult = authenticator.authenticate(params, request, dsl);
        boolean basicAuthAttempted = authenticator.hasBasicAuth(request);
        if (authResult == null) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed", basicAuthAttempted);
        }
        OidcApplication app = authResult.app();
        String clientId = app.clientId();

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

        // 7. Build tokens — decrypt private key if encrypted at rest
        KeyEncryptor encryptor = new KeyEncryptor(config.getKeyEncryptionKey(), config.getSecureRandom());
        byte[] privateKeyBytes = encryptor.decrypt(app.privateKey());
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
        String accessToken = RsaJwtSigner.sign(accessClaims, privateKeyBytes, kid);

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
        OidcClaimMapper.addUserClaims(idClaims, authCode.userId(), authCode.scope(), dsl);
        String idToken = RsaJwtSigner.sign(idClaims, privateKeyBytes, kid);

        // Zero out decrypted private key material
        Arrays.fill(privateKeyBytes, (byte) 0);

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
