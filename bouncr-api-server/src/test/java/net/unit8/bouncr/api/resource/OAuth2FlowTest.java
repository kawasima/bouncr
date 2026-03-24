package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.Problem;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders;
import net.unit8.bouncr.data.AuthorizeRequest;
import net.unit8.bouncr.data.TokenRequest;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.PkceChallenge;
import net.unit8.bouncr.data.Scope;
import net.unit8.bouncr.data.UserIdentity;
import net.unit8.bouncr.sign.RsaJwtSigner;
import net.unit8.bouncr.util.KeyEncryptor;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OAUTH2_REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-style tests for the full OAuth2 authorize -> token -> refresh flow
 * using a real H2 database and in-memory KVS stores.
 */
class OAuth2FlowTest {

    private DSLContext dsl;
    private BouncrConfiguration config;
    private StoreProvider storeProvider;
    private OAuth2AuthorizeResource authorizeResource;
    private OAuth2TokenResource tokenResource;
    private SecureRandom random;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        random = new SecureRandom();

        config = new BouncrConfiguration();
        config.setIssuerBaseUrl("https://issuer.example");
        config.setSecureRandom(random);
        config.setPbkdf2Iterations(1);

        storeProvider = new StoreProvider();

        authorizeResource = new OAuth2AuthorizeResource();
        setField(authorizeResource, "config", config);
        setField(authorizeResource, "storeProvider", storeProvider);

        tokenResource = new OAuth2TokenResource();
        setField(tokenResource, "config", config);
        setField(tokenResource, "storeProvider", storeProvider);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    // ==================== Test 1: authorization_code flow ====================

    @Test
    void authorizationCode_fullFlow() {
        OidcApplication app = createOidcApp("authcode_app",
                EnumSet.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                "https://webapp.example/callback");
        String clientSecret = "test-secret-1";
        setClientSecret(app, clientSecret);

        // Step 1: Authorization endpoint validates the request
        AuthorizeRequest authorizeReq = new AuthorizeRequest(
                "code", app.clientId(), "https://webapp.example/callback",
                Scope.parse("openid"), "state123", "nonce456", null);

        RestContext authCtx = restContext();
        boolean exists = authorizeResource.exists(authorizeReq, authCtx, dsl);
        assertThat(exists).isTrue();

        // Step 2: Simulate the authorize endpoint issuing a code
        long now = config.getClock().instant().getEpochSecond();
        UserIdentity user = new UserIdentity(1L, "testuser");
        String code = "test-auth-code-123";
        AuthorizationCode authCode = new AuthorizationCode(
                app.clientId(), user, Scope.parse("openid"),
                "nonce456", null, "https://webapp.example/callback", now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        // Step 3: Exchange code for tokens via token endpoint
        Parameters tokenParams = Parameters.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", "https://webapp.example/callback",
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext tokenCtx = restContext();
        Problem malformed = tokenResource.isMalformed(tokenParams, tokenCtx);
        assertThat(malformed).isNull();

        TokenRequest tokenReq = tokenCtx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow();

        boolean authorized = tokenResource.isAuthorized(tokenParams, httpRequest(), tokenCtx, dsl);
        assertThat(authorized).isTrue();

        OidcApplication authenticatedApp = tokenCtx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow();
        boolean posted = tokenResource.doPost(tokenReq, authenticatedApp, tokenCtx, dsl);
        assertThat(posted).isTrue();

        ApiResponse response = tokenCtx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("access_token");
        assertThat(body).containsKey("id_token");
        assertThat(body).containsKey("refresh_token");
        assertThat(body.get("token_type")).isEqualTo("Bearer");
        assertThat(body.get("scope")).isEqualTo("openid");

        // Step 4: Verify access_token is a valid JWT with correct claims
        String accessToken = (String) body.get("access_token");
        Map<String, Object> accessClaims = RsaJwtSigner.verify(accessToken, app.publicKey());
        assertThat(accessClaims).isNotNull();
        assertThat(accessClaims.get("iss")).isEqualTo("https://issuer.example/oauth2/openid/" + app.clientId());
        assertThat(accessClaims.get("sub")).isEqualTo("testuser");
        assertThat(accessClaims.get("aud")).isEqualTo(app.clientId());
        assertThat(accessClaims).containsKey("exp");
        assertThat(accessClaims.get("scope")).isEqualTo("openid");

        // Step 5: Verify id_token is a valid JWT with correct claims
        String idToken = (String) body.get("id_token");
        Map<String, Object> idClaims = RsaJwtSigner.verify(idToken, app.publicKey());
        assertThat(idClaims).isNotNull();
        assertThat(idClaims.get("iss")).isEqualTo("https://issuer.example/oauth2/openid/" + app.clientId());
        assertThat(idClaims.get("sub")).isEqualTo("testuser");
        assertThat(idClaims.get("aud")).isEqualTo(app.clientId());
        assertThat(idClaims.get("nonce")).isEqualTo("nonce456");

        // Step 6: Verify authorization code is consumed (single-use)
        assertThat(storeProvider.getStore(AUTHORIZATION_CODE).read(code)).isNull();
    }

    // ==================== Test 2: client_credentials flow ====================

    @Test
    void clientCredentials_returnsAccessTokenOnly() {
        OidcApplication app = createOidcApp("cc_app",
                EnumSet.of(GrantType.CLIENT_CREDENTIALS), null);
        String clientSecret = "cc-secret-1";
        setClientSecret(app, clientSecret);

        Parameters params = Parameters.of(
                "grant_type", "client_credentials",
                "scope", "openid",
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext ctx = restContext();
        Problem malformed = tokenResource.isMalformed(params, ctx);
        assertThat(malformed).isNull();

        TokenRequest tokenReq = ctx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow();

        boolean authorized = tokenResource.isAuthorized(params, httpRequest(), ctx, dsl);
        assertThat(authorized).isTrue();

        OidcApplication authenticatedApp = ctx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow();
        tokenResource.doPost(tokenReq, authenticatedApp, ctx, dsl);

        ApiResponse response = ctx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("access_token");
        assertThat(body).doesNotContainKey("refresh_token");
        assertThat(body).doesNotContainKey("id_token");

        // Verify access_token JWT has client_id as sub
        String accessToken = (String) body.get("access_token");
        Map<String, Object> claims = RsaJwtSigner.verify(accessToken, app.publicKey());
        assertThat(claims).isNotNull();
        assertThat(claims.get("sub")).isEqualTo(app.clientId());
        assertThat(claims.get("client_id")).isEqualTo(app.clientId());
    }

    // ==================== Test 3: refresh_token flow ====================

    @Test
    void refreshToken_rotatesAndIssuesNewAccessToken() {
        OidcApplication app = createOidcApp("refresh_app",
                EnumSet.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                "https://webapp.example/callback");
        String clientSecret = "refresh-secret-1";
        setClientSecret(app, clientSecret);

        // First, set up an authorization code flow to get a refresh token
        long now = config.getClock().instant().getEpochSecond();
        UserIdentity user = new UserIdentity(2L, "refreshuser");
        String code = "refresh-auth-code-123";
        AuthorizationCode authCode = new AuthorizationCode(
                app.clientId(), user, Scope.parse("openid"),
                null, null, "https://webapp.example/callback", now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        // Exchange code for tokens
        Parameters codeParams = Parameters.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", "https://webapp.example/callback",
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext codeCtx = restContext();
        tokenResource.isMalformed(codeParams, codeCtx);
        tokenResource.isAuthorized(codeParams, httpRequest(), codeCtx, dsl);
        tokenResource.doPost(
                codeCtx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                codeCtx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                codeCtx, dsl);

        @SuppressWarnings("unchecked")
        Map<String, Object> firstBody = (Map<String, Object>) codeCtx.get(OAuth2TokenResource.TOKEN_RESPONSE)
                .orElseThrow().getBody();
        String refreshToken = (String) firstBody.get("refresh_token");
        assertThat(refreshToken).isNotBlank();

        // Now use the refresh token to get new tokens
        Parameters refreshParams = Parameters.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext refreshCtx = restContext();
        tokenResource.isMalformed(refreshParams, refreshCtx);
        tokenResource.isAuthorized(refreshParams, httpRequest(), refreshCtx, dsl);
        tokenResource.doPost(
                refreshCtx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                refreshCtx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                refreshCtx, dsl);

        ApiResponse refreshResponse = refreshCtx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        assertThat(refreshResponse.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshBody = (Map<String, Object>) refreshResponse.getBody();
        assertThat(refreshBody).containsKey("access_token");
        assertThat(refreshBody).containsKey("refresh_token");
        String newRefreshToken = (String) refreshBody.get("refresh_token");
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // Verify old refresh token is deleted (single-use rotation)
        assertThat(storeProvider.getStore(OAUTH2_REFRESH_TOKEN).read(refreshToken)).isNull();

        // Verify new access_token is valid
        String newAccessToken = (String) refreshBody.get("access_token");
        Map<String, Object> claims = RsaJwtSigner.verify(newAccessToken, app.publicKey());
        assertThat(claims).isNotNull();
        assertThat(claims.get("sub")).isEqualTo("refreshuser");
    }

    // ==================== Test 4: PKCE (S256) ====================

    @Test
    void pkce_s256_success() {
        OidcApplication app = createOidcApp("pkce_app",
                EnumSet.of(GrantType.AUTHORIZATION_CODE),
                "https://webapp.example/callback");
        String clientSecret = "pkce-secret-1";
        setClientSecret(app, clientSecret);

        // Generate PKCE code_verifier and code_challenge
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String codeChallenge = computeS256Challenge(codeVerifier);
        PkceChallenge pkce = new PkceChallenge(codeChallenge, "S256");

        // Store authorization code with PKCE challenge
        long now = config.getClock().instant().getEpochSecond();
        UserIdentity user = new UserIdentity(3L, "pkceuser");
        String code = "pkce-auth-code-123";
        AuthorizationCode authCode = new AuthorizationCode(
                app.clientId(), user, Scope.parse("openid"),
                null, pkce, "https://webapp.example/callback", now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        // Exchange code with code_verifier
        Parameters params = Parameters.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", "https://webapp.example/callback",
                "code_verifier", codeVerifier,
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext ctx = restContext();
        tokenResource.isMalformed(params, ctx);
        tokenResource.isAuthorized(params, httpRequest(), ctx, dsl);
        tokenResource.doPost(
                ctx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                ctx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                ctx, dsl);

        ApiResponse response = ctx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("access_token");

        // Verify the JWT is valid
        Map<String, Object> claims = RsaJwtSigner.verify((String) body.get("access_token"), app.publicKey());
        assertThat(claims).isNotNull();
        assertThat(claims.get("sub")).isEqualTo("pkceuser");
    }

    @Test
    void pkce_s256_wrongVerifier_fails() {
        OidcApplication app = createOidcApp("pkce_fail_app",
                EnumSet.of(GrantType.AUTHORIZATION_CODE),
                "https://webapp.example/callback");
        String clientSecret = "pkce-fail-secret";
        setClientSecret(app, clientSecret);

        String codeVerifier = "correct-verifier-value";
        String codeChallenge = computeS256Challenge(codeVerifier);
        PkceChallenge pkce = new PkceChallenge(codeChallenge, "S256");

        long now = config.getClock().instant().getEpochSecond();
        UserIdentity user = new UserIdentity(4L, "pkceuser2");
        String code = "pkce-fail-code-123";
        AuthorizationCode authCode = new AuthorizationCode(
                app.clientId(), user, Scope.parse("openid"),
                null, pkce, "https://webapp.example/callback", now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        // Exchange with WRONG code_verifier
        Parameters params = Parameters.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", "https://webapp.example/callback",
                "code_verifier", "wrong-verifier-value",
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext ctx = restContext();
        tokenResource.isMalformed(params, ctx);
        tokenResource.isAuthorized(params, httpRequest(), ctx, dsl);
        tokenResource.doPost(
                ctx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                ctx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                ctx, dsl);

        ApiResponse response = ctx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.INVALID_GRANT.getValue());
        assertThat((String) body.get("error_description")).contains("PKCE");
    }

    // ==================== Test 5: unauthorized grant type rejected ====================

    @Test
    void clientCredentialsOnly_rejectsAuthorizationCode_atAuthorizeEndpoint() {
        OidcApplication app = createOidcApp("cc_only_app2",
                EnumSet.of(GrantType.CLIENT_CREDENTIALS), null);

        AuthorizeRequest authorizeReq = new AuthorizeRequest(
                "code", app.clientId(), "https://attacker.example/callback",
                Scope.parse("openid"), null, null, null);

        RestContext ctx = restContext();
        boolean exists = authorizeResource.exists(authorizeReq, ctx, dsl);
        assertThat(exists).isFalse();

        ApiResponse response = authorizeResource.handleNotFound(ctx);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.UNAUTHORIZED_CLIENT.getValue());
    }

    @Test
    void clientCredentialsOnly_rejectsAuthorizationCode_atTokenEndpoint() {
        OidcApplication app = createOidcApp("cc_only_token_app",
                EnumSet.of(GrantType.CLIENT_CREDENTIALS), null);
        String clientSecret = "cc-only-token-secret";
        setClientSecret(app, clientSecret);

        // Store a fake authorization code
        long now = config.getClock().instant().getEpochSecond();
        UserIdentity user = new UserIdentity(5L, "baduser");
        String code = "unauthorized-code-123";
        AuthorizationCode authCode = new AuthorizationCode(
                app.clientId(), user, Scope.parse("openid"),
                null, null, "https://webapp.example/callback", now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        Parameters params = Parameters.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", "https://webapp.example/callback",
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext ctx = restContext();
        tokenResource.isMalformed(params, ctx);
        tokenResource.isAuthorized(params, httpRequest(), ctx, dsl);
        tokenResource.doPost(
                ctx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                ctx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                ctx, dsl);

        ApiResponse response = ctx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.UNAUTHORIZED_CLIENT.getValue());
    }

    @Test
    void authorizationCodeOnly_rejectsRefreshToken() {
        OidcApplication app = createOidcApp("authcode_only_app",
                EnumSet.of(GrantType.AUTHORIZATION_CODE),
                "https://webapp.example/callback");
        String clientSecret = "authcode-only-secret";
        setClientSecret(app, clientSecret);

        // First get a refresh token by doing an auth code exchange
        long now = config.getClock().instant().getEpochSecond();
        UserIdentity user = new UserIdentity(6L, "norefreshuser");
        String code = "norefresh-code-123";
        AuthorizationCode authCode = new AuthorizationCode(
                app.clientId(), user, Scope.parse("openid"),
                null, null, "https://webapp.example/callback", now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        // Exchange the code (this works since AUTHORIZATION_CODE is allowed)
        Parameters codeParams = Parameters.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", "https://webapp.example/callback",
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext codeCtx = restContext();
        tokenResource.isMalformed(codeParams, codeCtx);
        tokenResource.isAuthorized(codeParams, httpRequest(), codeCtx, dsl);
        tokenResource.doPost(
                codeCtx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                codeCtx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                codeCtx, dsl);

        @SuppressWarnings("unchecked")
        Map<String, Object> firstBody = (Map<String, Object>) codeCtx.get(OAuth2TokenResource.TOKEN_RESPONSE)
                .orElseThrow().getBody();
        String refreshToken = (String) firstBody.get("refresh_token");
        assertThat(refreshToken).isNotBlank();

        // Now try to use refresh_token grant (not allowed for this app)
        Parameters refreshParams = Parameters.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", app.clientId(),
                "client_secret", clientSecret);

        RestContext refreshCtx = restContext();
        tokenResource.isMalformed(refreshParams, refreshCtx);
        tokenResource.isAuthorized(refreshParams, httpRequest(), refreshCtx, dsl);
        tokenResource.doPost(
                refreshCtx.get(OAuth2TokenResource.TOKEN_REQ).orElseThrow(),
                refreshCtx.get(OAuth2TokenResource.CLIENT_APP).orElseThrow(),
                refreshCtx, dsl);

        ApiResponse refreshResponse = refreshCtx.get(OAuth2TokenResource.TOKEN_RESPONSE).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> refreshBody = (Map<String, Object>) refreshResponse.getBody();
        assertThat(refreshBody.get("error")).isEqualTo(OAuth2Error.UNAUTHORIZED_CLIENT.getValue());
    }

    // ==================== Helpers ====================

    /**
     * Creates an OIDC application with an RSA key pair stored in DB.
     * Uses no-op KeyEncryptor (null key = dev mode passthrough).
     */
    private OidcApplication createOidcApp(String name, Set<GrantType> grantTypes, String callbackUrl) {
        KeyPair keyPair = KeyUtils.generate(2048, random);
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

        // Use no-op encryption (dev mode) for private key storage
        KeyEncryptor encryptor = new KeyEncryptor(null, random);
        byte[] storedPrivateKey = encryptor.encrypt(privateKeyBytes);

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.insert(
                name, "client-" + name, "placeholder-secret",
                storedPrivateKey, publicKeyBytes,
                null, callbackUrl, "Test app: " + name, null, null);
        repo.setGrantTypes(app.id(), EnumSet.copyOf(grantTypes));

        // Re-fetch to include grant types
        return repo.findByClientId(app.clientId()).orElseThrow();
    }

    /**
     * Sets the client secret for an OIDC application (hashed with PBKDF2).
     */
    private void setClientSecret(OidcApplication app, String rawSecret) {
        byte[] hash = PasswordUtils.pbkdf2(rawSecret, app.clientId(), config.getPbkdf2Iterations());
        String encoded = Base64.getEncoder().encodeToString(hash);
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.updateClientSecret(app.name(), encoded);
    }

    private String computeS256Challenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RestContext restContext() {
        Resource stubResource = decisionPoint -> ctx -> null;
        return new RestContext(stubResource, new DefaultHttpRequest());
    }

    private HttpRequest httpRequest() {
        DefaultHttpRequest req = new DefaultHttpRequest();
        req.setHeaders(Headers.empty());
        return req;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
