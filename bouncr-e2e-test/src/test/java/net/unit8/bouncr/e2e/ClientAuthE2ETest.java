package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAuthE2ETest extends E2ETestBase {
    private APIRequestContext adminApi;
    private OidcClient client;

    @BeforeAll
    void setupClient() throws Exception {
        adminApi = adminContext();
        client = createOidcApplication(adminApi, "client_auth_app");
    }

    @AfterAll
    void cleanupClient() {
        if (adminApi != null) {
            adminApi.dispose();
        }
    }

    @Test
    @Tag("e2e-smoke")
    void clientCredentials_issuesAccessTokenOnly() throws Exception {
        APIResponse response = clientCredentials(client, "openid");
        assertThat(response.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> tokens = JSON.readValue(response.body(), Map.class);
        assertThat(tokens.get("access_token")).isNotNull();
        assertThat(tokens.get("token_type")).isEqualTo("Bearer");
        assertThat(tokens).doesNotContainKey("refresh_token");
        assertThat(tokens).doesNotContainKey("id_token");
    }

    @Test
    @Tag("e2e-full")
    void tokenEndpoint_missingClientAuthentication_returns401InvalidClient() throws Exception {
        APIResponse response = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setData(formBody("grant_type", "client_credentials", "scope", "openid")));

        assertThat(response.status()).isEqualTo(401);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("invalid_client");
    }

    @Test
    @Tag("e2e-full")
    void tokenEndpoint_invalidClientSecret_returns401InvalidClient() throws Exception {
        APIResponse response = api.post("/oauth2/token",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setHeader("Authorization", basicAuth(client.clientId(), "invalid-secret"))
                        .setData(formBody("grant_type", "client_credentials", "scope", "openid")));

        assertThat(response.status()).isEqualTo(401);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("invalid_client");
    }
}
