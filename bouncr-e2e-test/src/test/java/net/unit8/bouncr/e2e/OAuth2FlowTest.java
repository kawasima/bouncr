package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E integration tests for the OIDC Identity Provider.
 * Starts a full Bouncr API server and tests OAuth2/OIDC flows via HTTP.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OAuth2FlowTest extends E2ETestBase {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    // Admin permissions from V23 migration
    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "oidc_application:read", "oidc_application:create",
            "oidc_application:update", "oidc_application:delete");

    // Shared state across ordered tests
    private static String clientId;
    private static String clientSecret;
    private static String accessToken;
    private static String refreshToken;
    private static String idToken;
    private static APIRequestContext adminApi;

    @BeforeAll
    void setupAdmin() {
        adminApi = authenticatedContext(1L, "admin", ADMIN_PERMISSIONS);
    }

    @AfterAll
    void cleanupAdmin() {
        if (adminApi != null) adminApi.dispose();
    }

    // ==================== OidcApplication CRUD ====================

    @Test @Order(1)
    void createOidcApplication_returnsClientCredentials() throws Exception {
        String payload = JSON.writeValueAsString(Map.of(
                "name", "e2e_test_app",
                "home_url", "http://localhost:9999",
                "callback_url", "http://localhost:9999/callback",
                "description", "E2E test application"));

        APIResponse response = adminApi.post("/bouncr/api/oidc_applications",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload));
        assertThat(response.status()).isEqualTo(201);
        byte[] body = response.body();
        assertThat(body).as("Response body should not be empty").isNotEmpty();

        Map<String, Object> app = JSON.readValue(body, Map.class);
        clientId = (String) app.get("client_id");
        clientSecret = (String) app.get("client_secret");
        assertThat(clientId).isNotNull().isNotBlank();
        assertThat(clientSecret).isNotNull().isNotBlank();
        // client_secret is shown only once (plaintext in creation response)
    }

    // ==================== Discovery + JWKS ====================

    @Test @Order(2)
    void discovery_returnsValidConfiguration() throws Exception {
        APIResponse response = api.get("/oauth2/openid/" + clientId + "/.well-known/openid-configuration");
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> config = JSON.readValue(response.body(), Map.class);
        assertThat(config.get("issuer")).isEqualTo(baseUrl + "/oauth2/openid/" + clientId);
        assertThat(config.get("authorization_endpoint")).isEqualTo(baseUrl + "/oauth2/authorize");
        assertThat(config.get("token_endpoint")).isEqualTo(baseUrl + "/oauth2/token");
        assertThat(config.get("userinfo_endpoint")).isEqualTo(baseUrl + "/oauth2/userinfo");
        assertThat(config.get("revocation_endpoint")).isEqualTo(baseUrl + "/oauth2/token/revoke");
        assertThat(config.get("introspection_endpoint")).isEqualTo(baseUrl + "/oauth2/token/introspect");
    }

    @Test @Order(3)
    void jwks_returnsPublicKey() throws Exception {
        APIResponse response = api.get("/oauth2/openid/" + clientId + "/certs");
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> jwks = JSON.readValue(response.body(), Map.class);
        var keys = (List<?>) jwks.get("keys");
        assertThat(keys).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, String> key = (Map<String, String>) keys.get(0);
        assertThat(key).containsKeys("kty", "use", "alg", "kid", "n", "e");
        assertThat(key.get("kty")).isEqualTo("RSA");
    }

    @Test @Order(4)
    void jwks_unknownClient_returns404() {
        APIResponse response = api.get("/oauth2/openid/nonexistent/certs");
        assertThat(response.status()).isEqualTo(404);
    }

    // ==================== Authorization Code Flow ====================

    @Test @Order(10)
    void authorizationCodeFlow_fullRoundTrip() throws Exception {
        // PKCE code verifier + challenge
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        // Authorization request (authenticated as admin via x-bouncr-credential)
        String authorizeUrl = "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + encode("http://localhost:9999/callback")
                + "&scope=" + encode("openid profile email")
                + "&state=xyz123"
                + "&nonce=nonce456"
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";

        APIResponse authResponse = adminApi.get(authorizeUrl,
                RequestOptions.create().setMaxRedirects(0));
        if (authResponse.status() != 302) {
            throw new AssertionError("Authorize failed: " + authResponse.status()
                    + " headers=" + authResponse.headers()
                    + " body=" + new String(authResponse.body()));
        }

        String location = authResponse.headers().get("location");
        assertThat(location).startsWith("http://localhost:9999/callback");
        assertThat(location).contains("code=");
        assertThat(location).contains("state=xyz123");

        // Extract authorization code
        Map<String, String> queryParams = parseQueryParams(URI.create(location).getQuery());
        String code = queryParams.get("code");
        assertThat(code).isNotNull().isNotBlank();

        // Exchange code for tokens
        String tokenBody = "grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode("http://localhost:9999/callback")
                + "&code_verifier=" + encode(codeVerifier);
        APIResponse tokenResponse = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(tokenBody));
        if (tokenResponse.status() != 200) {
            throw new AssertionError("Token exchange failed: " + tokenResponse.status()
                    + " " + new String(tokenResponse.body()));
        }

        Map<String, Object> tokens = JSON.readValue(tokenResponse.body(), Map.class);
        accessToken = (String) tokens.get("access_token");
        refreshToken = (String) tokens.get("refresh_token");
        idToken = (String) tokens.get("id_token");

        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
        assertThat(idToken).isNotNull();
        assertThat(tokens.get("token_type")).isEqualTo("Bearer");
        assertThat(tokens.get("scope")).isEqualTo("openid profile email");
    }

    // ==================== Refresh Token ====================

    @Test @Order(20)
    void refreshToken_rotationWorks() throws Exception {
        assertThat(refreshToken).as("refreshToken from auth code flow").isNotNull();

        APIResponse response = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("grant_type", "refresh_token", "refresh_token", refreshToken)));
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> tokens = JSON.readValue(response.body(), Map.class);
        String newAccessToken = (String) tokens.get("access_token");
        String newRefreshToken = (String) tokens.get("refresh_token");
        assertThat(newAccessToken).isNotNull().isNotEqualTo(accessToken);
        assertThat(newRefreshToken).isNotNull().isNotEqualTo(refreshToken);

        // Old refresh token should be invalid (rotation)
        APIResponse replayResponse = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("grant_type", "refresh_token", "refresh_token", refreshToken)));
        assertThat(replayResponse.status()).isEqualTo(400);

        // Update for subsequent tests
        accessToken = newAccessToken;
        refreshToken = newRefreshToken;
    }

    // ==================== client_credentials ====================

    @Test @Order(30)
    void clientCredentials_issuesAccessTokenOnly() throws Exception {
        APIResponse response = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("grant_type", "client_credentials", "scope", "openid")));
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> tokens = JSON.readValue(response.body(), Map.class);
        assertThat(tokens.get("access_token")).isNotNull();
        assertThat(tokens.get("token_type")).isEqualTo("Bearer");
        assertThat(tokens).doesNotContainKey("refresh_token");
        assertThat(tokens).doesNotContainKey("id_token");
    }

    // ==================== UserInfo ====================

    @Test @Order(40)
    void userInfo_returnsUserClaims() throws Exception {
        assertThat(accessToken).as("accessToken from refresh flow").isNotNull();

        APIResponse response = api.get("/oauth2/userinfo",
                RequestOptions.create()
                        .setHeader("Authorization", "Bearer " + accessToken));
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> userInfo = JSON.readValue(response.body(), Map.class);
        assertThat(userInfo.get("sub")).isEqualTo("admin");
    }

    // ==================== Token Introspection ====================

    @Test @Order(50)
    void introspection_validToken_returnsActive() throws Exception {
        assertThat(accessToken).isNotNull();

        APIResponse response = api.post("/oauth2/token/introspect",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("token", accessToken)));
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> result = JSON.readValue(response.body(), Map.class);
        assertThat(result.get("active")).isEqualTo(true);
        assertThat(result.get("sub")).isEqualTo("admin");
        assertThat(result.get("client_id")).isEqualTo(clientId);
    }

    @Test @Order(51)
    void introspection_invalidToken_returnsInactive() throws Exception {
        APIResponse response = api.post("/oauth2/token/introspect",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("token", "invalid.jwt.token")));
        assertThat(response.status()).isEqualTo(200);

        Map<String, Object> result = JSON.readValue(response.body(), Map.class);
        assertThat(result.get("active")).isEqualTo(false);
    }

    // ==================== Token Revocation ====================

    @Test @Order(60)
    void revocation_invalidatesRefreshToken() throws Exception {
        assertThat(refreshToken).as("refreshToken from refresh flow").isNotNull();

        APIResponse revokeResponse = api.post("/oauth2/token/revoke",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("token", refreshToken)));
        assertThat(revokeResponse.status()).isEqualTo(200);

        // Revoked refresh token should be invalid
        APIResponse refreshResponse = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(clientId, clientSecret))
                        .setData(formBody("grant_type", "refresh_token", "refresh_token", refreshToken)));
        assertThat(refreshResponse.status()).isEqualTo(400);

        Map<String, Object> error = JSON.readValue(refreshResponse.body(), Map.class);
        assertThat(error.get("error")).isEqualTo("invalid_grant");
    }

    // ==================== Helpers ====================

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Build application/x-www-form-urlencoded body from key-value pairs.
     */
    private static String formBody(String... pairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (sb.length() > 0) sb.append("&");
            sb.append(encode(pairs[i])).append("=").append(encode(pairs[i + 1]));
        }
        return sb.toString();
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }
}
