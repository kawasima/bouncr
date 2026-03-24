package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import enkan.collection.OptionMap;
import enkan.component.ApplicationComponent;
import enkan.component.builtin.HmacEncoder;
import enkan.component.flyway.FlywayMigration;
import enkan.component.hikaricp.HikariCPComponent;
import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.jetty.JettyComponent;
import enkan.component.jooq.JooqProvider;
import enkan.component.metrics.MetricsComponent;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.hook.GrantBouncrUserRole;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtHeader;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for E2E tests. Starts the Bouncr API server in-process
 * with H2 in-memory database and MemoryStore (no Redis required).
 *
 * <p>Authentication is simulated by setting the {@code x-bouncr-credential}
 * header with an HS256 JWT, equivalent to what bouncr-proxy sets after
 * token lookup in production.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class E2ETestBase {
    protected static final JsonMapper JSON = JsonMapper.builder().build();

    // Must match JWT_SECRET environment variable set in pom.xml surefire config
    protected static final String JWT_SECRET = "e2e-test-secret-key-32-bytes-ok";

    protected static final List<String> ADMIN_PERMISSIONS = List.of(
            "any_user:read", "any_user:create", "any_user:update", "any_user:delete",
            "any_group:read", "any_group:create", "any_group:update",
            "any_role:read", "any_role:create", "any_role:update", "any_role:delete",
            "any_permission:read", "any_permission:create", "any_permission:update", "any_permission:delete",
            "oidc_application:read", "oidc_application:create",
            "oidc_application:update", "oidc_application:delete");

    protected EnkanSystem system;
    protected String baseUrl;
    protected Playwright playwright;
    protected APIRequestContext api;

    @BeforeAll
    void startSystem() {
        system = createTestSystem();
        system.start();
        baseUrl = "http://localhost:13005";

        playwright = Playwright.create();
        api = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(baseUrl));
    }

    @AfterAll
    void stopSystem() {
        if (api != null) api.dispose();
        if (playwright != null) playwright.close();
        if (system != null) system.stop();
    }

    /**
     * Create an APIRequestContext with x-bouncr-credential header set
     * for the given user. This simulates bouncr-proxy's authentication.
     */
    protected APIRequestContext authenticatedContext(long userId, String account, List<String> permissions) {
        String credentialJwt = generateCredentialJwt(userId, account, permissions);
        return playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(baseUrl)
                .setExtraHTTPHeaders(Map.of("x-bouncr-credential", credentialJwt)));
    }

    protected APIRequestContext adminContext() {
        return authenticatedContext(1L, "admin", ADMIN_PERMISSIONS);
    }

    /**
     * Generate an x-bouncr-credential JWT (HS256) for the given user.
     */
    private String generateCredentialJwt(long userId, String account, List<String> permissions) {
        JsonWebToken jwt = system.getComponent("jwt");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("uid", String.valueOf(userId));
        claims.put("sub", account);
        claims.put("permissions", permissions);
        claims.put("iss", "bouncr");
        JwtHeader header = new JwtHeader("JWT", "HS256", null);
        return jwt.sign(claims, header, JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    protected String basicAuth(String clientId, String clientSecret) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    protected OidcClient createOidcApplication(APIRequestContext adminApi, String name) throws Exception {
        String callbackUri = "http://localhost:9999/callback/" + name;
        String payload = JSON.writeValueAsString(Map.of(
                "name", name,
                "grant_types", List.of("authorization_code", "client_credentials", "refresh_token"),
                "home_uri", "http://localhost:9999/" + name,
                "callback_uri", callbackUri,
                "description", "E2E test application: " + name));

        APIResponse response = adminApi.post("/bouncr/api/oidc_applications",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload));

        assertThat(response.status()).isEqualTo(201);
        @SuppressWarnings("unchecked")
        Map<String, Object> app = JSON.readValue(response.body(), Map.class);
        String clientId = (String) app.get("client_id");
        String clientSecret = (String) app.get("client_secret");
        assertThat(clientId).isNotNull().isNotBlank();
        assertThat(clientSecret).isNotNull().isNotBlank();
        return new OidcClient(clientId, clientSecret, callbackUri);
    }

    protected OidcClient createOidcApplication(APIRequestContext adminApi, String name,
                                               String backchannelLogoutUri, String frontchannelLogoutUri) throws Exception {
        String callbackUri = "http://localhost:9999/callback/" + name;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("home_uri", "http://localhost:9999/" + name);
        payload.put("callback_uri", callbackUri);
        payload.put("description", "E2E test application: " + name);
        if (backchannelLogoutUri != null) {
            payload.put("backchannel_logout_uri", backchannelLogoutUri);
        }
        if (frontchannelLogoutUri != null) {
            payload.put("frontchannel_logout_uri", frontchannelLogoutUri);
        }

        APIResponse response = adminApi.post("/bouncr/api/oidc_applications",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(JSON.writeValueAsString(payload)));

        assertThat(response.status()).isEqualTo(201);
        @SuppressWarnings("unchecked")
        Map<String, Object> app = JSON.readValue(response.body(), Map.class);
        String clientId = (String) app.get("client_id");
        String clientSecret = (String) app.get("client_secret");
        assertThat(clientId).isNotNull().isNotBlank();
        assertThat(clientSecret).isNotNull().isNotBlank();
        return new OidcClient(clientId, clientSecret, callbackUri);
    }

    protected String codeChallengeS256(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    protected String authorizeAndGetCode(APIRequestContext actorApi, OidcClient client,
                                         String scope, String state, String nonce,
                                         String codeChallenge) {
        String authorizeUrl = "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + urlEncode(client.clientId())
                + "&redirect_uri=" + urlEncode(client.callbackUri())
                + "&scope=" + urlEncode(scope)
                + "&state=" + urlEncode(state)
                + "&nonce=" + urlEncode(nonce)
                + "&code_challenge=" + urlEncode(codeChallenge)
                + "&code_challenge_method=S256";

        APIResponse authResponse = actorApi.get(authorizeUrl,
                RequestOptions.create().setMaxRedirects(0));
        assertThat(authResponse.status()).isEqualTo(302);

        String location = authResponse.headers().get("location");
        assertThat(location).startsWith(client.callbackUri());
        Map<String, String> queryParams = parseQueryParams(URI.create(location).getQuery());
        assertThat(queryParams.get("state")).isEqualTo(state);

        String code = queryParams.get("code");
        assertThat(code).isNotNull().isNotBlank();
        return code;
    }

    protected APIResponse exchangeAuthorizationCode(OidcClient client, String code, String codeVerifier) {
        return api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(client.clientId(), client.clientSecret()))
                        .setData(formBody(
                                "grant_type", "authorization_code",
                                "code", code,
                                "redirect_uri", client.callbackUri(),
                                "code_verifier", codeVerifier
                        )));
    }

    protected APIResponse refreshToken(OidcClient client, String refreshToken) {
        return api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(client.clientId(), client.clientSecret()))
                        .setData(formBody("grant_type", "refresh_token", "refresh_token", refreshToken)));
    }

    protected APIResponse introspect(OidcClient client, String token) {
        return api.post("/oauth2/token/introspect",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(client.clientId(), client.clientSecret()))
                        .setData(formBody("token", token)));
    }

    protected APIResponse revoke(OidcClient client, String token) {
        return api.post("/oauth2/token/revoke",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(client.clientId(), client.clientSecret()))
                        .setData(formBody("token", token)));
    }

    protected APIResponse clientCredentials(OidcClient client, String scope) {
        return api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(client.clientId(), client.clientSecret()))
                        .setData(formBody("grant_type", "client_credentials", "scope", scope)));
    }

    protected APIResponse postJson(APIRequestContext context, String path, Object payload) throws Exception {
        return context.post(path, RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData(JSON.writeValueAsString(payload)));
    }

    protected APIResponse putJson(APIRequestContext context, String path, Object payload) throws Exception {
        return context.put(path, RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData(JSON.writeValueAsString(payload)));
    }

    protected APIResponse deleteJson(APIRequestContext context, String path, Object payload) throws Exception {
        return context.delete(path, RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData(JSON.writeValueAsString(payload)));
    }

    protected Map<String, Object> parseJwtPayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        assertThat(parts.length).isGreaterThanOrEqualTo(2);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = JSON.readValue(payload, Map.class);
        return map;
    }

    protected static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected static String formBody(String... pairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (sb.length() > 0) sb.append("&");
            sb.append(urlEncode(pairs[i])).append("=").append(urlEncode(pairs[i + 1]));
        }
        return sb.toString();
    }

    protected static Map<String, String> parseQueryParams(String query) {
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

    protected record OidcClient(String clientId, String clientSecret, String callbackUri) {
    }

    private static EnkanSystem createTestSystem() {
        // Set enkan.env first — cors.origins default depends on it
        setPropertyIfAbsent("enkan.env", "development");

        String env = System.getProperty("enkan.env", "development");
        if ("development".equals(env)) {
            setPropertyIfAbsent("cors.origins", "*");
        } else {
            setPropertyIfAbsent("cors.origins", "http://localhost:13005");
        }
        setPropertyIfAbsent("internal.signing.key", "e2e-test-internal-signing-key!");

        String jdbcUrl = "jdbc:h2:mem:e2e_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

        BouncrConfiguration config = builder(new BouncrConfiguration())
                .set(BouncrConfiguration::setPasswordPolicy,
                        builder(new PasswordPolicy())
                                .set(PasswordPolicy::setExpires, Duration.ofDays(180))
                                .build())
                .set(BouncrConfiguration::setIssuerBaseUrl, "http://localhost:13005")
                .build();
        GrantBouncrUserRole grantBouncrUserRole = new GrantBouncrUserRole();
        config.getHookRepo().register(HookPoint.BEFORE_CREATE_USER, grantBouncrUserRole);
        config.getHookRepo().register(HookPoint.BEFORE_SIGN_UP, grantBouncrUserRole);

        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "config", config,
                "converter", new JacksonBeansConverter(),
                "jooq", builder(new JooqProvider())
                        .set(JooqProvider::setDialect, SQLDialect.H2)
                        .build(),
                "storeprovider", new StoreProvider(),
                "flake", new Flake(),
                "jwt", new JsonWebToken(),
                "realmCache", new RealmCache(),
                "flyway", builder(new FlywayMigration())
                        .set(FlywayMigration::setCleanBeforeMigration, true)
                        .build(),
                "metrics", new MetricsComponent(),
                "datasource", new HikariCPComponent(OptionMap.of("uri", jdbcUrl)),
                "app", new ApplicationComponent<>("net.unit8.bouncr.api.BouncrApplicationFactory"),
                "http", builder(new JettyComponent())
                        .set(JettyComponent::setPort, 13005)
                        .build()
        ).relationships(
                component("http").using("app"),
                component("app").using("config", "storeprovider", "realmCache", "jooq", "jwt",
                        "converter", "metrics"),
                component("storeprovider").using("config"),
                component("realmCache").using("jooq", "flyway"),
                component("jooq").using("datasource"),
                component("flyway").using("datasource"),
                component("jwt").using("config")
        );
    }

    private static void setPropertyIfAbsent(String key, String value) {
        String prop = System.getProperty(key);
        String envKey = key.replace('.', '_').toUpperCase();
        String env = System.getenv(envKey);
        if ((prop == null || prop.isBlank()) && (env == null || env.isBlank())) {
            System.setProperty(key, value);
        }
    }
}
