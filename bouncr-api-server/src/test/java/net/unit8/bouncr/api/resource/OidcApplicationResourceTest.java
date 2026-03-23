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
    private OidcApplicationsResoruce createResource;
    private OAuth2AuthorizeResource authorizeResource;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();

        BouncrConfiguration config = new BouncrConfiguration();
        config.setIssuerBaseUrl("https://issuer.example");
        config.setSecureRandom(new java.security.SecureRandom());
        config.setPbkdf2Iterations(1);

        createResource = new OidcApplicationsResoruce();
        setField(createResource, "config", config);

        authorizeResource = new OAuth2AuthorizeResource();
        setField(authorizeResource, "config", config);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    // ==================== Scenario 1: CLI / M2M application ====================
    // client_credentials only, no callback_url needed

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
                context.get(OidcApplicationsResoruce.CREATE_REQ).orElseThrow(),
                context, dsl);
        assertThat(created).isTrue();

        OidcApplicationCreatedResponse response = context.get(OidcApplicationsResoruce.RESPONSE).orElseThrow();
        assertThat(response.client_id()).isNotBlank();
        assertThat(response.client_secret()).isNotBlank();
        assertThat(response.grant_types()).containsExactly("client_credentials");
        assertThat(response.callback_url()).isNull();
    }

    @Test
    void create_clientCredentialsOnly_noCallbackUrl_dbRoundTrip() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "batch_job",
                  "grant_types": ["client_credentials"]
                }
                """);

        RestContext context = restContext();
        assertThat(createResource.validateCreate(body, context)).isNull();
        createResource.create(
                context.get(OidcApplicationsResoruce.CREATE_REQ).orElseThrow(),
                context, dsl);

        // Verify round-trip through DB
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication loaded = repo.findByName("batch_job").orElseThrow();
        assertThat(loaded.grantTypes()).containsExactly(GrantType.CLIENT_CREDENTIALS);
        assertThat(loaded.callbackUrl()).isNull();
        assertThat(loaded.homeUrl()).isNull();
    }

    // ==================== Scenario 2: Web application ====================
    // authorization_code + refresh_token, callback_url required

    @Test
    void create_authorizationCode_withCallbackUrl_succeeds() throws Exception {
        JsonNode body = JSON.readTree("""
                {
                  "name": "web_app",
                  "grant_types": ["authorization_code", "refresh_token"],
                  "callback_url": "https://webapp.example/callback",
                  "home_url": "https://webapp.example",
                  "description": "Web application"
                }
                """);

        RestContext context = restContext();
        assertThat(createResource.validateCreate(body, context)).isNull();
        createResource.create(
                context.get(OidcApplicationsResoruce.CREATE_REQ).orElseThrow(),
                context, dsl);

        OidcApplicationCreatedResponse response = context.get(OidcApplicationsResoruce.RESPONSE).orElseThrow();
        assertThat(response.grant_types()).containsExactlyInAnyOrder("authorization_code", "refresh_token");
        assertThat(response.callback_url()).isEqualTo("https://webapp.example/callback");
    }

    @Test
    void create_authorizationCode_withoutCallbackUrl_fails() throws Exception {
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
                .contains("/callback_url");
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
        var authorizeRequest = new net.unit8.bouncr.api.decoder.BouncrFormDecoders.AuthorizeRequest(
                "code", "cc-client-1", "https://attacker.example/callback",
                net.unit8.bouncr.data.Scope.parse("openid"), null, null, null);

        boolean exists = authorizeResource.exists(authorizeRequest, context, dsl);
        assertThat(exists).isFalse();

        ApiResponse response = authorizeResource.handleNotFound(context);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.UNAUTHORIZED_CLIENT.getValue());
    }

    // ==================== Helpers ====================

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
