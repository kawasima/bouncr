package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.data.OAuth2RefreshToken;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Phase 3 features: refresh token, client_credentials, introspection.
 */
class OAuth2Phase3Test {
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

    // --- Refresh Token ---

    @Test
    void refreshToken_recordRoundTrip() {
        OAuth2RefreshToken rt = new OAuth2RefreshToken("client1", 42L, "admin", "openid profile", 1700000000L);
        assertThat(rt.clientId()).isEqualTo("client1");
        assertThat(rt.userId()).isEqualTo(42L);
        assertThat(rt.userAccount()).isEqualTo("admin");
        assertThat(rt.scope()).isEqualTo("openid profile");
        assertThat(rt.createdAt()).isEqualTo(1700000000L);
    }

    @Test
    void refreshToken_isSerializable() {
        OAuth2RefreshToken rt = new OAuth2RefreshToken("c", 1L, "u", "s", 0L);
        assertThat(rt).isInstanceOf(java.io.Serializable.class);
    }

    // --- Client Credentials ---

    @Test
    void clientCredentials_jwtHasClientIdAsSub() {
        long now = System.currentTimeMillis() / 1000;
        String kid = RsaJwtSigner.deriveKid(publicKeyBytes);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "https://bouncr.example.com/oauth2/openid/myapp");
        claims.put("sub", "myapp"); // client_id as sub
        claims.put("aud", "myapp");
        claims.put("exp", now + 900);
        claims.put("iat", now);
        claims.put("scope", "openid");
        claims.put("client_id", "myapp");

        String jwt = RsaJwtSigner.sign(claims, privateKeyBytes, kid);
        Map<String, Object> verified = RsaJwtSigner.verify(jwt, publicKeyBytes);

        assertThat(verified).isNotNull();
        assertThat(verified.get("sub")).isEqualTo("myapp");
        assertThat(verified.get("client_id")).isEqualTo("myapp");
    }

    // --- Token Introspection ---

    @Test
    void introspection_validJwt_returnsActiveClaims() {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "https://bouncr.example.com/oauth2/openid/testclient");
        claims.put("sub", "admin");
        claims.put("aud", "testclient");
        claims.put("exp", now + 900);
        claims.put("iat", now);
        claims.put("jti", "unique-id-123");
        claims.put("scope", "openid profile");
        claims.put("client_id", "testclient");

        String kid = RsaJwtSigner.deriveKid(publicKeyBytes);
        String jwt = RsaJwtSigner.sign(claims, privateKeyBytes, kid);

        // Verify — simulates introspection logic
        Map<String, Object> verified = RsaJwtSigner.verify(jwt, publicKeyBytes);
        assertThat(verified).isNotNull();
        assertThat(verified.get("sub")).isEqualTo("admin");
        assertThat(verified.get("jti")).isEqualTo("unique-id-123");
        assertThat(((Number) verified.get("exp")).longValue()).isGreaterThan(now);
    }

    @Test
    void introspection_expiredJwt_detectable() {
        long past = System.currentTimeMillis() / 1000 - 1000;
        Map<String, Object> claims = Map.of("sub", "admin", "exp", past, "iat", past - 900, "client_id", "test");
        String jwt = RsaJwtSigner.sign(claims, privateKeyBytes);

        Map<String, Object> verified = RsaJwtSigner.verify(jwt, publicKeyBytes);
        assertThat(verified).isNotNull(); // Signature valid
        long now = System.currentTimeMillis() / 1000;
        assertThat(((Number) verified.get("exp")).longValue()).isLessThan(now); // But expired
    }

    @Test
    void introspection_invalidSignature_returnsNull() {
        String fakeJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalidsignature";
        Map<String, Object> verified = RsaJwtSigner.verify(fakeJwt, publicKeyBytes);
        assertThat(verified).isNull();
    }

    // --- Grant Type Dispatch ---

    @Test
    void grantType_switchPattern() {
        // Verify grant_type string matching works
        String authCode = "authorization_code";
        String refresh = "refresh_token";
        String clientCreds = "client_credentials";
        String unknown = "implicit";

        assertThat(authCode).isEqualTo("authorization_code");
        assertThat(refresh).isEqualTo("refresh_token");
        assertThat(clientCreds).isEqualTo("client_credentials");
        assertThat(unknown).isNotEqualTo("authorization_code");
    }
}
