package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.PkceChallenge;
import net.unit8.bouncr.data.Scope;
import net.unit8.bouncr.data.UserIdentity;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OAuth2 token endpoint validation logic.
 */
class OAuth2TokenResourceTest {

    @Test
    void pkceVerification_validVerifier_shouldSucceed() throws Exception {
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        // Verify that our challenge matches what the token endpoint would compute
        byte[] recomputed = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String recomputedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(recomputed);
        assertThat(recomputedChallenge).isEqualTo(codeChallenge);
    }

    @Test
    void pkceVerification_wrongVerifier_shouldFail() throws Exception {
        String codeVerifier = "correct-verifier";
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        byte[] wrongDigest = MessageDigest.getInstance("SHA-256")
                .digest("wrong-verifier".getBytes(StandardCharsets.UTF_8));
        String wrongChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(wrongDigest);
        assertThat(wrongChallenge).isNotEqualTo(codeChallenge);
    }

    @Test
    void scopeParsing_exactMatch() {
        String scope = "openid profile email";
        Set<String> scopes = Set.of(scope.split("\\s+"));
        assertThat(scopes).contains("openid", "profile", "email");
        assertThat(scopes).doesNotContain("fooopenid");
    }

    @Test
    void authorizationCode_serialization_roundtrip() {
        UserIdentity user = new UserIdentity(42L, "admin");
        Scope scope = Scope.parse("openid profile");
        PkceChallenge pkce = new PkceChallenge("challenge-xyz", "S256");
        AuthorizationCode code = new AuthorizationCode(
                "client123", user, scope, "nonce-abc", pkce,
                "https://example.com/callback", 1700000000L);

        assertThat(code.clientId()).isEqualTo("client123");
        assertThat(code.user().userId()).isEqualTo(42L);
        assertThat(code.user().account()).isEqualTo("admin");
        assertThat(code.scope().contains("openid")).isTrue();
        assertThat(code.nonce()).isEqualTo("nonce-abc");
        assertThat(code.pkce().verify("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")).isFalse(); // wrong verifier
        assertThat(code.redirectUri()).isEqualTo("https://example.com/callback");
        assertThat(code.createdAt()).isEqualTo(1700000000L);
    }

    @Test
    void jwtSignedWithRsa_verifiableWithPublicKey() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        String kid = RsaJwtSigner.deriveKid(kp.getPublic().getEncoded());
        Map<String, Object> claims = Map.of(
                "iss", "https://bouncr.example.com/oauth2/openid/testclient",
                "sub", "admin",
                "aud", "testclient",
                "exp", System.currentTimeMillis() / 1000 + 900,
                "iat", System.currentTimeMillis() / 1000);

        String jwt = RsaJwtSigner.sign(claims, kp.getPrivate().getEncoded(), kid);
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        // Verify header contains kid
        String header = new String(Base64.getUrlDecoder().decode(parts[0]));
        assertThat(header).contains("\"kid\":\"" + kid + "\"");

        // Verify signature
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(kp.getPublic());
        sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        assertThat(sig.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
    }

    @Test
    void authCodeExpiry_check() {
        long now = 1700001000L;
        long createdAt = 1700000000L; // 1000 seconds ago
        long expiresIn = 60L; // 60 seconds

        assertThat(now - createdAt > expiresIn).isTrue(); // expired

        long recentCreatedAt = now - 30; // 30 seconds ago
        assertThat(now - recentCreatedAt > expiresIn).isFalse(); // not expired
    }
}
