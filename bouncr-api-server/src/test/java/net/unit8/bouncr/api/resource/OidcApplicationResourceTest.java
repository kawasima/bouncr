package net.unit8.bouncr.api.resource;

import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.Problem;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.OidcApplicationCreatedResponse;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OidcApplication;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-style tests for OidcApplication creation with real H2 database.
 * Exercises the full path: JSON decode → validation → DB insert → response.
 */
class OidcApplicationResourceTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private DSLContext dsl;
    private OidcApplicationsResource createResource;
    private OAuth2AuthorizeResource authorizeResource;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();

        BouncrConfiguration config = createConfig();

        createResource = new OidcApplicationsResource();
        setField(createResource, "config", config);

        authorizeResource = new OAuth2AuthorizeResource();
        setField(authorizeResource, "config", config);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    // ==================== Scenario 1: CLI / M2M application ====================
    // client_credentials only, no callback_uri needed

    @Test
    void create_clientCredentialsOnly_succeeds() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "cli_tool",
                  "grant_types": ["client_credentials"],
                  "description": "CLI automation"
                }
                """);

        RestContext context = restContext();
        Problem problem = createResource.validateCreate(body, context);
        assertThat(problem).isNull();

        boolean created = createResource.create(
                context.get(OidcApplicationsResource.CREATE_REQ).orElseThrow(),
                context, dsl);
        assertThat(created).isTrue();

        OidcApplicationCreatedResponse response = context.get(OidcApplicationsResource.RESPONSE).orElseThrow();
        assertThat(response.client_id()).isNotBlank();
        assertThat(response.client_secret()).isNotBlank();
        assertThat(response.grant_types()).containsExactly("client_credentials");
        assertThat(response.callback_uri()).isNull();
    }

    @Test
    void create_clientCredentialsOnly_noCallbackUri_dbRoundTrip() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "batch_job",
                  "grant_types": ["client_credentials"]
                }
                """);

        RestContext context = restContext();
        assertThat(createResource.validateCreate(body, context)).isNull();
        createResource.create(
                context.get(OidcApplicationsResource.CREATE_REQ).orElseThrow(),
                context, dsl);

        // Verify round-trip through DB
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication loaded = repo.findByName("batch_job").orElseThrow();
        assertThat(loaded.metadata().grantTypes()).containsExactly(GrantType.CLIENT_CREDENTIALS);
        assertThat(loaded.metadata().callbackUri()).isNull();
        assertThat(loaded.metadata().homeUri()).isNull();
    }

    // ==================== Scenario 2: Web application ====================
    // authorization_code + refresh_token, callback_uri required

    @Test
    void create_authorizationCode_withCallbackUri_succeeds() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "web_app",
                  "grant_types": ["authorization_code", "refresh_token"],
                  "callback_uri": "https://webapp.example/callback",
                  "home_uri": "https://webapp.example",
                  "description": "Web application"
                }
                """);

        RestContext context = restContext();
        assertThat(createResource.validateCreate(body, context)).isNull();
        createResource.create(
                context.get(OidcApplicationsResource.CREATE_REQ).orElseThrow(),
                context, dsl);

        OidcApplicationCreatedResponse response = context.get(OidcApplicationsResource.RESPONSE).orElseThrow();
        assertThat(response.grant_types()).containsExactlyInAnyOrder("authorization_code", "refresh_token");
        assertThat(response.callback_uri()).isEqualTo("https://webapp.example/callback");
    }

    @Test
    void create_authorizationCode_withoutCallbackUri_fails() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "broken_app",
                  "grant_types": ["authorization_code"],
                  "description": "Missing callback"
                }
                """);

        RestContext context = restContext();
        Problem problem = createResource.validateCreate(body, context);

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getViolations()).isNotEmpty();
        assertThat(problem.getViolations().stream().map(Problem.Violation::field))
                .contains("/callback_uri");
    }

    // ==================== Scenario 3: Empty grant_types rejected ====================

    @Test
    void create_emptyGrantTypes_fails() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "no_grants",
                  "grant_types": [],
                  "description": "No grants"
                }
                """);

        RestContext context = restContext();
        Problem problem = createResource.validateCreate(body, context);

        assertThat(problem).isNotNull();
        assertThat(problem.getViolations().stream().map(Problem.Violation::field))
                .contains("/grant_types");
    }

    // ==================== Scenario 4: Token endpoint enforces grant_type ====================

    @Test
    void authorize_clientCredentialsOnly_rejectsAuthorizationCode() throws Exception {
        // Create a client_credentials-only application
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.insert(
                "cc_only_app", "cc-client-1", "secret",
                new byte[]{1}, new byte[]{2},
                null, null, null, null, null);
        repo.setGrantTypes(app.id(), EnumSet.of(GrantType.CLIENT_CREDENTIALS));

        // Try to use authorization_code flow
        RestContext context = restContext();
        var authorizeRequest = new net.unit8.bouncr.data.AuthorizeRequest(
                "code", "cc-client-1", "https://attacker.example/callback",
                net.unit8.bouncr.data.Scope.parse("openid"), null, null, null);

        boolean exists = authorizeResource.exists(authorizeRequest, context, dsl);
        assertThat(exists).isFalse();

        ApiResponse response = authorizeResource.handleNotFound(context);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.UNAUTHORIZED_CLIENT.getValue());
    }

    // ==================== Scenario 5: Secret regeneration ====================

    @Test
    void regenerateSecret_producesNewWorkingSecret() throws Exception {
        // Create an application
        JsonNode body = JSON.readTree("""
                {
                  "name": "regen_test",
                  "grant_types": ["client_credentials"]
                }
                """);
        RestContext createCtx = restContext();
        createResource.validateCreate(body, createCtx);
        createResource.create(
                createCtx.get(OidcApplicationsResource.CREATE_REQ).orElseThrow(),
                createCtx, dsl);
        OidcApplicationCreatedResponse created = createCtx.get(OidcApplicationsResource.RESPONSE).orElseThrow();
        String originalSecret = created.client_secret();

        // Regenerate secret
        OidcApplicationSecretResource secretResource = new OidcApplicationSecretResource();
        setField(secretResource, "config", createConfig());

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByName("regen_test").orElseThrow();

        RestContext regenCtx = restContext();
        secretResource.regenerate(app, regenCtx, dsl);
        String newSecret = regenCtx.get(OidcApplicationSecretResource.NEW_SECRET).orElseThrow();

        assertThat(newSecret).isNotBlank();
        assertThat(newSecret).isNotEqualTo(originalSecret);

        // Verify the new secret matches the stored hash
        OidcApplication updated = repo.findByClientId(app.credentials().clientId()).orElseThrow();
        byte[] newHash = net.unit8.bouncr.util.PasswordUtils.pbkdf2(newSecret, app.credentials().clientId(), 1);
        byte[] storedHash = java.util.Base64.getDecoder().decode(updated.credentials().clientSecret());
        assertThat(java.security.MessageDigest.isEqual(newHash, storedHash)).isTrue();

        // Verify old secret no longer matches
        byte[] oldHash = net.unit8.bouncr.util.PasswordUtils.pbkdf2(originalSecret, app.credentials().clientId(), 1);
        assertThat(java.security.MessageDigest.isEqual(oldHash, storedHash)).isFalse();
    }

    // ==================== Scenario 6: Unknown grant type rejected ====================

    @Test
    void create_unknownGrantType_fails() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "bogus_app",
                  "grant_types": ["bogus"]
                }
                """);
        RestContext context = restContext();
        Problem problem = createResource.validateCreate(body, context);

        assertThat(problem).isNotNull();
        assertThat(problem.getViolations().stream().map(Problem.Violation::field))
                .contains("/grant_types");
    }

    // ==================== Helpers ====================

    private BouncrConfiguration createConfig() {
        BouncrConfiguration config = new BouncrConfiguration();
        config.setIssuerBaseUrl("https://issuer.example");
        config.setSecureRandom(new java.security.SecureRandom());
        config.setPbkdf2Iterations(1);
        return config;
    }

    private RestContext restContext() {
        Resource stubResource = decisionPoint -> ctx -> null;
        return new RestContext(stubResource, new DefaultHttpRequest());
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
