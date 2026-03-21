package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

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
    // Must match JWT_SECRET environment variable set in pom.xml surefire config
    protected static final String JWT_SECRET = "e2e-test-secret-key-32-bytes-ok";

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

    private static EnkanSystem createTestSystem() {
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
}
