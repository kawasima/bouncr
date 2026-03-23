package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e-smoke")
class OidcMetadataE2ETest extends E2ETestBase {
    private APIRequestContext adminApi;
    private OidcClient client;

    @BeforeAll
    void setupClient() throws Exception {
        adminApi = adminContext();
        client = createOidcApplication(adminApi, "meta_app");
    }

    @AfterAll
    void cleanupClient() {
        if (adminApi != null) {
            adminApi.dispose();
        }
    }

    @Test
    void discovery_returnsExpectedEndpoints() throws Exception {
        APIResponse response = api.get("/oauth2/openid/" + client.clientId() + "/.well-known/openid-configuration");
        assertThat(response.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> config = JSON.readValue(response.body(), Map.class);
        assertThat(config.get("issuer")).isEqualTo(baseUrl + "/oauth2/openid/" + client.clientId());
        assertThat(config.get("authorization_endpoint")).isEqualTo(baseUrl + "/oauth2/authorize");
        assertThat(config.get("token_endpoint")).isEqualTo(baseUrl + "/oauth2/token");
        assertThat(config.get("userinfo_endpoint")).isEqualTo(baseUrl + "/oauth2/userinfo");
        assertThat(config.get("revocation_endpoint")).isEqualTo(baseUrl + "/oauth2/token/revoke");
        assertThat(config.get("introspection_endpoint")).isEqualTo(baseUrl + "/oauth2/token/introspect");
    }

    @Test
    void jwks_returnsSingleRsaPublicKey() throws Exception {
        APIResponse response = api.get("/oauth2/openid/" + client.clientId() + "/certs");
        assertThat(response.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> jwks = JSON.readValue(response.body(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> keys = (List<Map<String, String>>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0)).containsKeys("kty", "use", "alg", "kid", "n", "e");
        assertThat(keys.get(0).get("kty")).isEqualTo("RSA");
    }

    @Test
    void jwks_unknownClient_returns404() {
        APIResponse response = api.get("/oauth2/openid/nonexistent/certs");
        assertThat(response.status()).isEqualTo(404);
    }
}
