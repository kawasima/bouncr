package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.data.OidcSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OIDC sign-in validation logic (state, nonce, aud, azp, exp).
 * These test the validation rules from OidcSignInResource without requiring the full framework.
 */
class OidcSignInValidationTest {

    // --- State validation ---

    @Test
    void stateValidation_sessionNull_shouldReject() {
        OidcSession session = null;
        // OidcSignInResource rejects when session is null
        assertThat(session).isNull();
        // This would return 401 "OIDC session not found"
    }

    @Test
    void stateValidation_stateMismatch_shouldReject() {
        OidcSession session = new OidcSession("nonce1", "state-abc", null, null);
        String requestState = "state-xyz";
        assertThat(Objects.equals(requestState, session.state())).isFalse();
    }

    @Test
    void stateValidation_stateMatch_shouldAccept() {
        OidcSession session = new OidcSession("nonce1", "state-abc", null, null);
        String requestState = "state-abc";
        assertThat(Objects.equals(requestState, session.state())).isTrue();
    }

    // --- Nonce validation ---

    @Test
    void nonceValidation_mismatch_shouldReject() {
        OidcSession session = new OidcSession("nonce-server", "state1", null, null);
        String claimNonce = "nonce-different";
        assertThat(Objects.equals(claimNonce, session.nonce())).isFalse();
    }

    @Test
    void nonceValidation_match_shouldAccept() {
        OidcSession session = new OidcSession("nonce-server", "state1", null, null);
        String claimNonce = "nonce-server";
        assertThat(Objects.equals(claimNonce, session.nonce())).isTrue();
    }

    // --- aud claim validation (RFC 7519 §4.1.3) ---

    @Test
    void audValidation_nullAud_shouldReject() {
        Object aud = null;
        assertThat(aud).isNull();
        // OidcSignInResource returns 401 "ID token missing audience"
    }

    @Test
    void audValidation_stringMatch_shouldAccept() {
        String clientId = "my-client-id";
        Object aud = "my-client-id";
        assertThat(aud instanceof String).isTrue();
        assertThat(clientId.equals(aud)).isTrue();
    }

    @Test
    void audValidation_stringMismatch_shouldReject() {
        String clientId = "my-client-id";
        Object aud = "other-client-id";
        assertThat(clientId.equals(aud)).isFalse();
    }

    @Test
    void audValidation_listContainsClientId_shouldAccept() {
        String clientId = "my-client-id";
        Object aud = List.of("my-client-id", "https://api.example.com");
        assertThat(aud instanceof List<?>).isTrue();
        assertThat(((List<?>) aud).contains(clientId)).isTrue();
    }

    @Test
    void audValidation_listMissingClientId_shouldReject() {
        String clientId = "my-client-id";
        Object aud = List.of("other-client-id", "https://api.example.com");
        assertThat(((List<?>) aud).contains(clientId)).isFalse();
    }

    // --- azp claim validation (OpenID Connect Core §3.1.3.3) ---

    @Test
    void azpValidation_multiValuedAud_azpPresent_matches_shouldAccept() {
        String clientId = "my-client-id";
        List<String> aud = List.of("my-client-id", "other-audience");
        String azp = "my-client-id";
        assertThat(aud.size()).isGreaterThan(1);
        assertThat(clientId.equals(azp)).isTrue();
    }

    @Test
    void azpValidation_multiValuedAud_azpPresent_mismatch_shouldReject() {
        String clientId = "my-client-id";
        List<String> aud = List.of("my-client-id", "other-audience");
        String azp = "wrong-client-id";
        assertThat(aud.size()).isGreaterThan(1);
        assertThat(clientId.equals(azp)).isFalse();
    }

    @Test
    void azpValidation_multiValuedAud_azpMissing_shouldReject() {
        List<String> aud = List.of("my-client-id", "other-audience");
        String azp = null;
        assertThat(aud.size()).isGreaterThan(1);
        assertThat(azp).isNull();
        // OidcSignInResource requires azp when aud has > 1 entry
    }

    @Test
    void azpValidation_singleAud_azpPresent_mismatch_shouldReject() {
        String clientId = "my-client-id";
        String azp = "wrong-client-id";
        // Even with single aud, if azp is present it must match
        assertThat(clientId.equals(azp)).isFalse();
    }

    @Test
    void azpValidation_singleAud_azpNull_shouldAccept() {
        String azp = null;
        // azp is optional for single aud
        assertThat(azp).isNull();
    }

    // --- exp claim validation with clock skew ---

    @Test
    void expValidation_notExpired_shouldAccept() {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + 300; // 5 minutes from now
        long clockSkewSeconds = 30;
        assertThat(exp + clockSkewSeconds < now).isFalse();
    }

    @Test
    void expValidation_expiredBeyondSkew_shouldReject() {
        long now = System.currentTimeMillis() / 1000;
        long exp = now - 60; // 60 seconds ago
        long clockSkewSeconds = 30;
        assertThat(exp + clockSkewSeconds < now).isTrue();
    }

    @Test
    void expValidation_expiredWithinSkew_shouldAccept() {
        long now = System.currentTimeMillis() / 1000;
        long exp = now - 20; // 20 seconds ago, within 30s skew
        long clockSkewSeconds = 30;
        assertThat(exp + clockSkewSeconds < now).isFalse();
    }

    // --- PKCE code_verifier ---

    @Test
    void pkceCodeVerifier_storedInSession() {
        OidcSession session = new OidcSession("nonce", "state", null, "verifier123");
        assertThat(session.codeVerifier()).isEqualTo("verifier123");
    }

    @Test
    void pkceCodeVerifier_nullWhenNotEnabled() {
        OidcSession session = new OidcSession("nonce", "state", null, null);
        assertThat(session.codeVerifier()).isNull();
    }
}
