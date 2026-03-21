package net.unit8.bouncr.data;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PkceChallengeTest {

    @Test
    void verify_matchingVerifier_returnsTrue() throws Exception {
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        PkceChallenge pkce = new PkceChallenge(challenge, "S256");
        assertThat(pkce.verify(codeVerifier)).isTrue();
    }

    @Test
    void verify_wrongVerifier_returnsFalse() throws Exception {
        String codeVerifier = "correct-verifier";
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        PkceChallenge pkce = new PkceChallenge(challenge, "S256");
        assertThat(pkce.verify("wrong-verifier")).isFalse();
    }

    @Test
    void verify_nullVerifier_returnsFalse() throws Exception {
        PkceChallenge pkce = new PkceChallenge("some-challenge", "S256");
        assertThat(pkce.verify(null)).isFalse();
    }

    @Test
    void constructor_nonS256Method_throws() {
        assertThatThrownBy(() -> new PkceChallenge("challenge", "plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("S256");
    }

    @Test
    void constructor_blankChallenge_throws() {
        assertThatThrownBy(() -> new PkceChallenge("", "S256"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
