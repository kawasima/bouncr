package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SignOutLogoutE2ETest extends E2ETestBase {
    private APIRequestContext adminApi;

    @BeforeAll
    void setupClient() throws Exception {
        adminApi = adminContext();
        createOidcApplication(
                adminApi,
                "logout_notify_app",
                "https://example.invalid/backchannel-logout",
                "https://rp.example/frontchannel-logout");
    }

    @AfterAll
    void cleanupClient() {
        if (adminApi != null) {
            adminApi.dispose();
        }
    }

    @Test
    @Tag("e2e-full")
    void signOut_returnsLogoutDispatchSummary() throws Exception {
        // Sign in via password to create a real session token in the store
        APIResponse signInResponse = postJson(api, "/bouncr/api/sign_in",
                Map.of("account", "admin", "password", "password"));
        assertThat(signInResponse.status()).isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> session = JSON.readValue(signInResponse.body(), Map.class);
        String token = (String) session.get("token");
        assertThat(token).isNotBlank();

        // Sign out using the authenticated context (x-bouncr-credential JWT),
        // passing the session token as the path parameter.
        // In E2E tests there is no bouncr-proxy, so Bearer token auth is not
        // available — we must use x-bouncr-credential for authentication.
        APIResponse signOutResponse = adminApi.delete("/bouncr/api/session/" + token);
        assertThat(signOutResponse.status())
                .as("Sign-out response: %s", new String(signOutResponse.body()))
                .isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(signOutResponse.body(), Map.class);

        @SuppressWarnings("unchecked")
        List<String> frontchannelUrls = (List<String>) body.get("frontchannel_logout_urls");
        assertThat(frontchannelUrls)
                .as("body=%s", body)
                .isNotNull()
                .contains("https://rp.example/frontchannel-logout");

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) body.get("backchannel_logout");
        assertThat(((Number) summary.get("attempted")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) summary.get("failed")).intValue()).isGreaterThanOrEqualTo(1);
    }
}
