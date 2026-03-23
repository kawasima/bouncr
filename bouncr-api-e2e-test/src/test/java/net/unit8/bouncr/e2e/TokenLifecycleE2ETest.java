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

class TokenLifecycleE2ETest extends E2ETestBase {
    private APIRequestContext adminApi;
    private OidcClient clientA;
    private OidcClient clientB;

    @BeforeAll
    void setupClients() throws Exception {
        adminApi = adminContext();
        clientA = createOidcApplication(adminApi, "token_lifecycle_a");
        clientB = createOidcApplication(adminApi, "token_lifecycle_b");
    }

    @AfterAll
    void cleanupClients() {
        if (adminApi != null) {
            adminApi.dispose();
        }
    }

    @Test
    @Tag("e2e-smoke")
    void refreshRotation_thenRevocation_invalidatesRefreshToken() throws Exception {
        TokenSet initial = issueTokensForAdmin(clientA, "rotation-1");

        APIResponse refreshResponse = refreshToken(clientA, initial.refreshToken());
        assertThat(refreshResponse.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshed = JSON.readValue(refreshResponse.body(), Map.class);
        String rotatedRefreshToken = (String) refreshed.get("refresh_token");
        String rotatedAccessToken = (String) refreshed.get("access_token");
        assertThat(rotatedRefreshToken).isNotNull().isNotEqualTo(initial.refreshToken());
        assertThat(rotatedAccessToken).isNotNull().isNotEqualTo(initial.accessToken());

        APIResponse replayOldRefresh = refreshToken(clientA, initial.refreshToken());
        assertThat(replayOldRefresh.status()).isEqualTo(400);

        APIResponse revokeResponse = revoke(clientA, rotatedRefreshToken);
        assertThat(revokeResponse.status()).isEqualTo(200);

        APIResponse refreshAfterRevoke = refreshToken(clientA, rotatedRefreshToken);
        assertThat(refreshAfterRevoke.status()).isEqualTo(400);

        @SuppressWarnings("unchecked")
        Map<String, Object> error = JSON.readValue(refreshAfterRevoke.body(), Map.class);
        assertThat(error.get("error")).isEqualTo("invalid_grant");
    }

    @Test
    @Tag("e2e-smoke")
    void introspection_validToken_returnsActiveForIssuingClient() throws Exception {
        TokenSet tokenSet = issueTokensForAdmin(clientA, "introspect-ok");

        APIResponse response = introspect(clientA, tokenSet.accessToken());
        assertThat(response.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("active")).isEqualTo(true);
        assertThat(body.get("client_id")).isEqualTo(clientA.clientId());
        assertThat(body.get("sub")).isEqualTo("admin");
    }

    @Test
    @Tag("e2e-full")
    void introspection_crossClient_returnsInactive() throws Exception {
        TokenSet tokenSet = issueTokensForAdmin(clientA, "introspect-cross");

        APIResponse response = introspect(clientB, tokenSet.accessToken());
        assertThat(response.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("active")).isEqualTo(false);
    }

    @Test
    @Tag("e2e-full")
    void introspection_invalidToken_returnsInactive() throws Exception {
        APIResponse response = introspect(clientA, "invalid.jwt.token");
        assertThat(response.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("active")).isEqualTo(false);
    }

    @Test
    @Tag("e2e-full")
    void userInfo_missingBearer_returns401() throws Exception {
        APIResponse response = api.get("/oauth2/userinfo");
        assertThat(response.status()).isEqualTo(401);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("invalid_token");
    }

    @Test
    @Tag("e2e-full")
    void userInfo_invalidToken_returns401() throws Exception {
        APIResponse response = api.get("/oauth2/userinfo",
                RequestOptions.create().setHeader("Authorization", "Bearer invalid.jwt.token"));
        assertThat(response.status()).isEqualTo(401);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("invalid_token");
    }

    private TokenSet issueTokensForAdmin(OidcClient client, String suffix) throws Exception {
        String codeVerifier = "verifier-" + suffix;
        String codeChallenge = codeChallengeS256(codeVerifier);

        String code = authorizeAndGetCode(adminApi, client,
                "openid profile email", "state-" + suffix, "nonce-" + suffix, codeChallenge);

        APIResponse tokenResponse = exchangeAuthorizationCode(client, code, codeVerifier);
        assertThat(tokenResponse.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> tokens = JSON.readValue(tokenResponse.body(), Map.class);
        return new TokenSet((String) tokens.get("access_token"), (String) tokens.get("refresh_token"));
    }

    private record TokenSet(String accessToken, String refreshToken) {
    }
}
