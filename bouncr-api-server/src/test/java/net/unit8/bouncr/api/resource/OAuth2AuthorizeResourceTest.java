package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.data.DefaultHttpRequest;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.Problem;
import kotowari.restful.data.Resource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.AuthorizeRequest;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.Scope;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AuthorizeResourceTest {
    private DSLContext dsl;
    private OAuth2AuthorizeResource resource;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        resource = new OAuth2AuthorizeResource();
        BouncrConfiguration config = new BouncrConfiguration();
        config.setIssuerBaseUrl("https://issuer.example");
        setField(resource, "config", config);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void malformed_missingRequiredParams_returnsProblem() {
        RestContext context = restContext();

        Problem problem = resource.isMalformed(Parameters.empty(), context);

        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getViolations()).isNotEmpty();
        assertThat(problem.getViolations().stream().map(Problem.Violation::field))
                .contains("/response_type", "/client_id", "/redirect_uri", "/scope");
    }

    @Test
    void exists_unknownClient_returnsInvalidClientErrorViaHandleNotFound() {
        RestContext context = restContext();
        AuthorizeRequest request = new AuthorizeRequest(
                "code",
                "unknown-client",
                "https://client.example/callback",
                Scope.parse("openid"),
                "st",
                null,
                null);

        boolean exists = resource.exists(request, context, dsl);

        assertThat(exists).isFalse();

        ApiResponse response = resource.handleNotFound(context);
        assertThat(response.getStatus()).isEqualTo(OAuth2Error.INVALID_CLIENT.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.INVALID_CLIENT.getValue());
    }

    @Test
    void exists_redirectUriMismatch_returnsInvalidRequestErrorViaHandleNotFound() {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.insert(
                "test-app",
                "client-1",
                "secret-1",
                new byte[]{1},
                new byte[]{2},
                "https://client.example",
                "https://client.example/callback",
                "test app",
                null,
                null);

        RestContext context = restContext();
        AuthorizeRequest request = new AuthorizeRequest(
                "code",
                "client-1",
                "https://client.example/other",
                Scope.parse("openid"),
                null,
                null,
                null);

        boolean exists = resource.exists(request, context, dsl);

        assertThat(exists).isFalse();

        ApiResponse response = resource.handleNotFound(context);
        assertThat(response.getStatus()).isEqualTo(OAuth2Error.INVALID_REQUEST.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo(OAuth2Error.INVALID_REQUEST.getValue());
        assertThat(body.get("error_description")).contains("redirect_uri");
    }

    @Test
    void handleUnauthorized_redirectsToSignInWithReturnUrl() {
        AuthorizeRequest request = new AuthorizeRequest(
                "code",
                "client-1",
                "https://client.example/callback",
                Scope.parse("openid profile"),
                "state-123",
                "nonce-456",
                null);

        ApiResponse response = resource.handleUnauthorized(request);

        assertThat(response.getStatus()).isEqualTo(302);
        String location = response.getHeaders().get("Location");
        assertThat(location).startsWith("https://issuer.example/sign_in?return_url=");
        assertThat(location).contains("%2foauth2%2fauthorize%3f");
        assertThat(location).contains("response_type%3dcode");
        assertThat(location).contains("client_id%3dclient-1");
        assertThat(location).contains("state%3dstate-123");
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
