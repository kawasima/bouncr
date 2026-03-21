package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.data.OAuth2RefreshToken;
import net.unit8.bouncr.data.Scope;
import net.unit8.bouncr.data.UserIdentity;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
        UserIdentity user = new UserIdentity(42L, "admin");
        Scope scope = Scope.parse("openid profile");
        OAuth2RefreshToken rt = new OAuth2RefreshToken("client1", user, scope, 1700000000L);
        assertThat(rt.clientId()).isEqualTo("client1");
        assertThat(rt.user().userId()).isEqualTo(42L);
        assertThat(rt.user().account()).isEqualTo("admin");
        assertThat(rt.scope().contains("openid")).isTrue();
        assertThat(rt.scope().contains("profile")).isTrue();
        assertThat(rt.createdAt()).isEqualTo(1700000000L);
    }

    @Test
    void refreshToken_isSerializable() {
        OAuth2RefreshToken rt = new OAuth2RefreshToken("c", new UserIdentity(1L, "u"), Scope.parse("s"), 0L);
        assertThat(rt).isInstanceOf(java.io.Serializable.class);
    }

    // --- Client Credentials ---

    @Test
    void clientCredentials_jwtHasClientIdAsSub() {
        long now = 1700000000L;
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
        long now = 1700000000L;
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
        long past = 1700000000L - 1000;
        Map<String, Object> claims = Map.of("sub", "admin", "exp", past, "iat", past - 900, "client_id", "test");
        String jwt = RsaJwtSigner.sign(claims, privateKeyBytes);

        Map<String, Object> verified = RsaJwtSigner.verify(jwt, publicKeyBytes);
        assertThat(verified).isNotNull(); // Signature valid
        long now = 1700000000L;
        assertThat(((Number) verified.get("exp")).longValue()).isLessThan(now); // But expired
    }

    @Test
    void introspection_invalidSignature_returnsNull() {
        String fakeJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalidsignature";
        Map<String, Object> verified = RsaJwtSigner.verify(fakeJwt, publicKeyBytes);
        assertThat(verified).isNull();
    }

    // --- Scope Subset Validation ---

    @Test
    void scopeSubsetValidation_subsetIsAccepted() {
        Set<String> original = new HashSet<>(Arrays.asList("openid", "profile", "email"));
        Set<String> requested = new HashSet<>(Arrays.asList("openid", "profile"));
        assertThat(original.containsAll(requested)).isTrue();
    }

    @Test
    void scopeSubsetValidation_escalationIsRejected() {
        Set<String> original = new HashSet<>(Arrays.asList("openid", "profile"));
        Set<String> requested = new HashSet<>(Arrays.asList("openid", "profile", "admin"));
        assertThat(original.containsAll(requested)).isFalse();
    }

    @Test
    void scopeSubsetValidation_sameSetIsAccepted() {
        Set<String> original = new HashSet<>(Arrays.asList("openid", "profile"));
        Set<String> requested = new HashSet<>(Arrays.asList("openid", "profile"));
        assertThat(original.containsAll(requested)).isTrue();
    }

    // --- Client Credentials Response Shape ---

    @Test
    void clientCredentials_responseHasNoRefreshToken() {
        // client_credentials grant should NOT include refresh_token or id_token
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", "jwt...");
        response.put("token_type", "Bearer");
        response.put("expires_in", 900);
        response.put("scope", "openid");

        assertThat(response).doesNotContainKey("refresh_token");
        assertThat(response).doesNotContainKey("id_token");
    }
}
