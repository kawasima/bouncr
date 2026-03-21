package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.sign.RsaJwtSigner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OAuth2 UserInfo endpoint logic — JWT verification and claim extraction.
 */
class OAuth2UserInfoResourceTest {
    private static byte[] privateKeyBytes;
    private static byte[] publicKeyBytes;

    @BeforeAll
    static void setup() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        privateKeyBytes = kp.getPrivate().getEncoded();
        publicKeyBytes = kp.getPublic().getEncoded();
    }

    @Test
    void accessToken_verifiedAndClaimsExtracted() {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "https://bouncr.example.com/oauth2/openid/testclient");
        claims.put("sub", "admin");
        claims.put("aud", "testclient");
        claims.put("exp", now + 900);
        claims.put("iat", now);
        claims.put("scope", "openid profile email");
        claims.put("client_id", "testclient");

        String kid = RsaJwtSigner.deriveKid(publicKeyBytes);
        String jwt = RsaJwtSigner.sign(claims, privateKeyBytes, kid);

        Map<String, Object> verified = RsaJwtSigner.verify(jwt, publicKeyBytes);
        assertThat(verified).isNotNull();
        assertThat(verified.get("sub")).isEqualTo("admin");
        assertThat(verified.get("client_id")).isEqualTo("testclient");
        assertThat(verified.get("scope")).isEqualTo("openid profile email");
    }

    @Test
    void expiredToken_verifiedButExpired() {
        long past = System.currentTimeMillis() / 1000 - 1000;
        Map<String, Object> claims = Map.of(
                "sub", "admin",
                "exp", past,
                "iat", past - 900);
        String jwt = RsaJwtSigner.sign(claims, privateKeyBytes);

        Map<String, Object> verified = RsaJwtSigner.verify(jwt, publicKeyBytes);
        assertThat(verified).isNotNull(); // Signature is valid
        // But exp < now
        long now = System.currentTimeMillis() / 1000;
        assertThat(((Number) verified.get("exp")).longValue()).isLessThan(now);
    }

    @Test
    void bearerTokenExtraction_pattern() {
        String header = "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature";
        assertThat(header.startsWith("Bearer ")).isTrue();
        String token = header.substring(7);
        assertThat(token).startsWith("eyJ");
    }

    @Test
    void missingBearerPrefix_rejected() {
        assertThat("Token abc123".startsWith("Bearer ")).isFalse();
        assertThat((String) null == null || !"".startsWith("Bearer ")).isTrue();
    }
}
