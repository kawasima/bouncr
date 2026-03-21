package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.TokenRequest;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.TokenRequest.AuthorizationCodeGrant;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.TokenRequest.ClientCredentialsGrant;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.TokenRequest.RefreshTokenGrant;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.api.service.OidcClaimMapper;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OAuth2RefreshToken;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.Scope;
import net.unit8.bouncr.sign.RsaJwtSigner;
import net.unit8.bouncr.util.KeyEncryptor;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OAUTH2_REFRESH_TOKEN;

/**
 * OAuth2 Token endpoint (Bouncr as OIDC IdP).
 * {@code POST /oauth2/token} ({@code application/x-www-form-urlencoded})
 *
 * <p>Follows the kotowari-restful Decision pattern:
 * <ol>
 *   <li>{@code MALFORMED} — decodes form parameters via {@link BouncrFormDecoders#TOKEN_REQUEST}</li>
 *   <li>{@code AUTHORIZED} — authenticates the OAuth2 client</li>
 *   <li>{@code POST} — executes the grant type logic</li>
 *   <li>{@code HANDLE_CREATED} — returns the token response</li>
 * </ol>
 *
 * <p>Supports {@code authorization_code}, {@code refresh_token}, and
 * {@code client_credentials} grant types via a {@code sealed interface}
 * dispatched in the {@code POST} decision.
 */
@AllowedMethods("POST")
public class OAuth2TokenResource {
    private static final Logger LOG = LoggerFactory.getLogger(OAuth2TokenResource.class);

    static final ContextKey<TokenRequest> TOKEN_REQ = ContextKey.of("tokenRequest", TokenRequest.class);
    static final ContextKey<OidcApplication> CLIENT_APP = ContextKey.of(OidcApplication.class);
    static final ContextKey<ApiResponse> TOKEN_RESPONSE = ContextKey.of("tokenResponse", ApiResponse.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    /**
     * Decodes and validates the form parameters using Raoh's {@link BouncrFormDecoders#TOKEN_REQUEST}.
     * On success, stores the typed {@link TokenRequest} in the context.
     * On failure, returns a {@link Problem} with field-level violations.
     */
    @Decision(value = MALFORMED, method = "POST")
    public Problem isMalformed(Parameters params, RestContext context) {
        return switch (BouncrFormDecoders.TOKEN_REQUEST.decode(params)) {
            case Ok<TokenRequest> ok -> {
                context.put(TOKEN_REQ, ok.value());
                yield null;
            }
            case Err<TokenRequest> err -> {
                List<Problem.Violation> violations = err.issues().asList().stream()
                        .map(issue -> new Problem.Violation(
                                issue.path().toString(), issue.code(), issue.message()))
                        .toList();
                yield Problem.fromViolationList(violations);
            }
        };
    }

    /**
     * Authenticates the OAuth2 client via HTTP Basic or POST body credentials.
     * Stores the authenticated {@link OidcApplication} in the context.
     */
    @Decision(AUTHORIZED)
    public boolean isAuthorized(Parameters params, HttpRequest request,
                                RestContext context, DSLContext dsl) {
        OAuth2ClientAuthenticator authenticator = new OAuth2ClientAuthenticator(config);
        OAuth2ClientAuthenticator.AuthResult authResult = authenticator.authenticate(params, request, dsl);
        if (authResult == null) {
            return false;
        }
        context.put(CLIENT_APP, authResult.app());
        return true;
    }

    /**
     * Dispatches to the appropriate grant type handler via sealed interface pattern matching.
     */
    @Decision(POST)
    public boolean doPost(TokenRequest tokenRequest, OidcApplication oidcApplication,
                          RestContext context, DSLContext dsl) {
        String clientId = oidcApplication.clientId();
        ApiResponse response = switch (tokenRequest) {
            case AuthorizationCodeGrant grant -> handleAuthorizationCode(grant, oidcApplication, clientId, dsl);
            case RefreshTokenGrant grant -> handleRefreshToken(grant, oidcApplication, clientId);
            case ClientCredentialsGrant grant -> handleClientCredentials(grant, oidcApplication, clientId);
        };
        context.put(TOKEN_RESPONSE, response);
        return true;
    }

    /**
     * Returns the token response (success or OAuth2 error) built by the grant handler.
     */
    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(ApiResponse tokenResponse) {
        return tokenResponse;
    }

    // ==================== Grant Type Handlers ====================

    private ApiResponse handleAuthorizationCode(AuthorizationCodeGrant grant,
                                                OidcApplication app, String clientId, DSLContext dsl) {
        Serializable stored = storeProvider.getStore(AUTHORIZATION_CODE).read(grant.code());
        if (stored == null) return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code is invalid or expired");
        storeProvider.getStore(AUTHORIZATION_CODE).delete(grant.code());

        if (!(stored instanceof AuthorizationCode authCode))
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code data is corrupt");

        long now = config.getClock().instant().getEpochSecond();
        if (now - authCode.createdAt() > config.getAuthorizationCodeExpires())
            return tokenError(OAuth2Error.INVALID_GRANT, "Authorization code has expired");
        if (!clientId.equals(authCode.clientId()))
            return tokenError(OAuth2Error.INVALID_GRANT, "Code was not issued to this client");
        if (!Objects.equals(grant.redirectUri(), authCode.redirectUri()))
            return tokenError(OAuth2Error.INVALID_GRANT, "redirect_uri does not match");

        if (authCode.pkce() != null) {
            if (grant.codeVerifier() == null || !authCode.pkce().verify(grant.codeVerifier())) {
                return tokenError(OAuth2Error.INVALID_GRANT, "PKCE verification failed");
            }
        }

        byte[] privateKeyBytes = decryptPrivateKey(app);
        try {
            String issuer = issuer(clientId);
            String kid = RsaJwtSigner.deriveKid(app.publicKey());
            String scopeStr = authCode.scope().toString();

            String accessToken = signAccessToken(issuer, authCode.user().account(), clientId, scopeStr, kid, privateKeyBytes, now);

            Map<String, Object> idClaims = new LinkedHashMap<>();
            idClaims.put("iss", issuer);
            idClaims.put("sub", authCode.user().account());
            idClaims.put("aud", clientId);
            idClaims.put("exp", now + config.getIdTokenExpires());
            idClaims.put("iat", now);
            if (authCode.nonce() != null) {
                idClaims.put("nonce", authCode.nonce());
            }
            OidcClaimMapper.addUserClaims(idClaims, authCode.user().userId(), scopeStr, dsl);
            String idToken = RsaJwtSigner.sign(idClaims, privateKeyBytes, kid);

            String refreshToken = UUID.randomUUID().toString();
            OAuth2RefreshToken refreshData = new OAuth2RefreshToken(
                    clientId, authCode.user(), authCode.scope(), now);
            storeProvider.getStore(OAUTH2_REFRESH_TOKEN).write(refreshToken, refreshData);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", config.getAccessTokenExpires());
            response.put("refresh_token", refreshToken);
            response.put("id_token", idToken);
            response.put("scope", scopeStr);
            return tokenResponse(response);
        } finally {
            Arrays.fill(privateKeyBytes, (byte) 0);
        }
    }

    private ApiResponse handleRefreshToken(RefreshTokenGrant grant,
                                           OidcApplication app, String clientId) {
        Serializable stored = storeProvider.getStore(OAUTH2_REFRESH_TOKEN).read(grant.refreshToken());
        if (!(stored instanceof OAuth2RefreshToken refreshData))
            return tokenError(OAuth2Error.INVALID_GRANT, "Refresh token is invalid or expired");
        if (!clientId.equals(refreshData.clientId()))
            return tokenError(OAuth2Error.INVALID_GRANT, "Refresh token was not issued to this client");

        Scope scope = grant.scope() != null ? grant.scope() : refreshData.scope();
        if (grant.scope() != null && !grant.scope().isSubsetOf(refreshData.scope())) {
            return tokenError(OAuth2Error.INVALID_SCOPE, "Requested scope exceeds originally granted scope");
        }

        long now = config.getClock().instant().getEpochSecond();
        byte[] privateKeyBytes = decryptPrivateKey(app);
        try {
            String issuer = issuer(clientId);
            String kid = RsaJwtSigner.deriveKid(app.publicKey());
            String scopeStr = scope.toString();

            String accessToken = signAccessToken(issuer, refreshData.user().account(), clientId, scopeStr, kid, privateKeyBytes, now);

            String newRefreshToken = UUID.randomUUID().toString();
            OAuth2RefreshToken newRefreshData = new OAuth2RefreshToken(
                    clientId, refreshData.user(), scope, now);
            storeProvider.getStore(OAUTH2_REFRESH_TOKEN).write(newRefreshToken, newRefreshData);
            storeProvider.getStore(OAUTH2_REFRESH_TOKEN).delete(grant.refreshToken());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", config.getAccessTokenExpires());
            response.put("refresh_token", newRefreshToken);
            response.put("scope", scopeStr);
            return tokenResponse(response);
        } finally {
            Arrays.fill(privateKeyBytes, (byte) 0);
        }
    }

    private ApiResponse handleClientCredentials(ClientCredentialsGrant grant,
                                                OidcApplication app, String clientId) {
        Scope scope = grant.scope();
        if (app.permissions() != null && !app.permissions().isEmpty()) {
            Set<String> allowedScopes = new HashSet<>();
            allowedScopes.add("openid");
            app.permissions().forEach(p -> allowedScopes.add(p.name()));
            if (!allowedScopes.containsAll(scope.values())) {
                return tokenError(OAuth2Error.INVALID_SCOPE,
                        "Requested scope exceeds client's registered permissions");
            }
        }

        long now = config.getClock().instant().getEpochSecond();
        byte[] privateKeyBytes = decryptPrivateKey(app);
        try {
            String issuer = issuer(clientId);
            String kid = RsaJwtSigner.deriveKid(app.publicKey());
            String scopeStr = scope.toString();

            String accessToken = signAccessToken(issuer, clientId, clientId, scopeStr, kid, privateKeyBytes, now);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", config.getAccessTokenExpires());
            response.put("scope", scopeStr);
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
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) {
            body.put("error_description", description);
        }
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, body)
                .build();
    }
}
