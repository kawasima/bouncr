package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCodeFlowE2ETest extends E2ETestBase {
    private APIRequestContext adminApi;
    private OidcClient client;

    @BeforeAll
    void setupClient() throws Exception {
        adminApi = adminContext();
        client = createOidcApplication(adminApi, "auth_code_app");
    }

    @AfterAll
    void cleanupClient() {
        if (adminApi != null) {
            adminApi.dispose();
        }
    }

    @Test
    @Tag("e2e-smoke")
    void authorizationCodeFlow_roundTrip_preservesStateAndNonce() throws Exception {
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String codeChallenge = codeChallengeS256(codeVerifier);
        String nonce = "nonce-e2e-1";

        String code = authorizeAndGetCode(adminApi, client,
                "openid profile email", "state-e2e-1", nonce, codeChallenge);

        APIResponse tokenResponse = exchangeAuthorizationCode(client, code, codeVerifier);
        assertThat(tokenResponse.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> tokens = JSON.readValue(tokenResponse.body(), Map.class);
        assertThat(tokens.get("access_token")).isNotNull();
        assertThat(tokens.get("refresh_token")).isNotNull();
        assertThat(tokens.get("id_token")).isNotNull();
        assertThat(tokens.get("token_type")).isEqualTo("Bearer");

        @SuppressWarnings("unchecked")
        Map<String, Object> idClaims = parseJwtPayload((String) tokens.get("id_token"));
        assertThat(idClaims.get("nonce")).isEqualTo(nonce);
    }

    @Test
    @Tag("e2e-full")
    void authorize_invalidRedirectUri_returnsInvalidRequest() {
        String authorizeUrl = "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + urlEncode(client.clientId())
                + "&redirect_uri=" + urlEncode("http://localhost:9999/wrong-callback")
                + "&scope=" + urlEncode("openid")
                + "&state=st1";

        APIResponse response = adminApi.get(authorizeUrl);
        assertThat(response.status()).isEqualTo(400);
    }

    @Test
    @Tag("e2e-full")
    void authorize_unsupportedResponseType_returnsError() throws Exception {
        String authorizeUrl = "/oauth2/authorize"
                + "?response_type=" + urlEncode("token")
                + "&client_id=" + urlEncode(client.clientId())
                + "&redirect_uri=" + urlEncode(client.callbackUrl())
                + "&scope=" + urlEncode("openid")
                + "&state=st2";

        APIResponse response = adminApi.get(authorizeUrl);
        assertThat(response.status()).isEqualTo(400);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(response.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("unsupported_response_type");
    }

    @Test
    @Tag("e2e-full")
    void tokenExchange_wrongCodeVerifier_returnsInvalidGrant() throws Exception {
        String codeVerifier = "correct-verifier-value";
        String codeChallenge = codeChallengeS256(codeVerifier);

        String code = authorizeAndGetCode(adminApi, client,
                "openid profile", "state-e2e-2", "nonce-e2e-2", codeChallenge);

        APIResponse tokenResponse = exchangeAuthorizationCode(client, code, "wrong-verifier");
        assertThat(tokenResponse.status()).isEqualTo(400);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(tokenResponse.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("invalid_grant");
    }

    @Test
    @Tag("e2e-full")
    void authorizationCode_replayReturnsInvalidGrant() throws Exception {
        String codeVerifier = "replay-verifier-value";
        String codeChallenge = codeChallengeS256(codeVerifier);

        String code = authorizeAndGetCode(adminApi, client,
                "openid profile", "state-e2e-3", "nonce-e2e-3", codeChallenge);

        APIResponse first = exchangeAuthorizationCode(client, code, codeVerifier);
        assertThat(first.status()).isEqualTo(200);

        APIResponse replay = exchangeAuthorizationCode(client, code, codeVerifier);
        assertThat(replay.status()).isEqualTo(400);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = JSON.readValue(replay.body(), Map.class);
        assertThat(body.get("error")).isEqualTo("invalid_grant");
    }

    @Test
    @Tag("e2e-full")
    void authorize_includesStateInRedirect() {
        String codeVerifier = "state-verifier-value";
        String state = "state-preserved-xyz";

        String authorizeUrl = "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + urlEncode(client.clientId())
                + "&redirect_uri=" + urlEncode(client.callbackUrl())
                + "&scope=" + urlEncode("openid profile")
                + "&state=" + urlEncode(state)
                + "&nonce=nx"
                + "&code_challenge=" + urlEncode(codeChallengeS256Unchecked(codeVerifier))
                + "&code_challenge_method=S256";

        APIResponse authResponse = adminApi.get(authorizeUrl,
                RequestOptions.create().setMaxRedirects(0));
        assertThat(authResponse.status()).isEqualTo(302);

        String location = authResponse.headers().get("location");
        Map<String, String> queryParams = parseQueryParams(URI.create(location).getQuery());
        assertThat(queryParams.get("state")).isEqualTo(state);
    }

    private String codeChallengeS256Unchecked(String verifier) {
        try {
            return codeChallengeS256(verifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
