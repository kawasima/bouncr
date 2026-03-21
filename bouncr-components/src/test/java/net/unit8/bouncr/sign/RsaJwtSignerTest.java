package net.unit8.bouncr.sign;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RsaJwtSignerTest {
    private static byte[] privateKeyBytes;
    private static byte[] publicKeyBytes;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        privateKeyBytes = kp.getPrivate().getEncoded();
        publicKeyBytes = kp.getPublic().getEncoded();
    }

    @Test
    void sign_producesValidJwt() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "testuser");
        payload.put("iss", "https://bouncr.example.com");

        String jwt = RsaJwtSigner.sign(payload, privateKeyBytes);

        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        // Decode header
        String header = new String(Base64.getUrlDecoder().decode(parts[0]));
        assertThat(header).contains("\"alg\":\"RS256\"");
        assertThat(header).contains("\"typ\":\"JWT\"");

        // Decode payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payloadJson).contains("\"sub\":\"testuser\"");
        assertThat(payloadJson).contains("\"iss\":\"https://bouncr.example.com\"");
    }

    @Test
    void sign_withKid_includesKidInHeader() {
        Map<String, Object> payload = Map.of("sub", "testuser");
        String jwt = RsaJwtSigner.sign(payload, privateKeyBytes, "my-kid-123");

        String header = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]));
        assertThat(header).contains("\"kid\":\"my-kid-123\"");
    }

    @Test
    void sign_signatureIsVerifiable() throws Exception {
        Map<String, Object> payload = Map.of("sub", "testuser", "iat", 1234567890L);
        String jwt = RsaJwtSigner.sign(payload, privateKeyBytes);

        String[] parts = jwt.split("\\.");
        String signingInput = parts[0] + "." + parts[1];
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
        java.security.PublicKey pubKey = kf.generatePublic(
                new java.security.spec.X509EncodedKeySpec(publicKeyBytes));

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubKey);
        sig.update(signingInput.getBytes());
        assertThat(sig.verify(signatureBytes)).isTrue();
    }

    @Test
    void deriveKid_returnsConsistentValue() {
        String kid1 = RsaJwtSigner.deriveKid(publicKeyBytes);
        String kid2 = RsaJwtSigner.deriveKid(publicKeyBytes);
        assertThat(kid1).isEqualTo(kid2);
        assertThat(kid1).hasSize(8);
    }

    @Test
    void publicKeyToJwk_containsRequiredFields() {
        String kid = RsaJwtSigner.deriveKid(publicKeyBytes);
        Map<String, String> jwk = RsaJwtSigner.publicKeyToJwk(publicKeyBytes, kid);

        assertThat(jwk).containsEntry("kty", "RSA");
        assertThat(jwk).containsEntry("use", "sig");
        assertThat(jwk).containsEntry("alg", "RS256");
        assertThat(jwk).containsEntry("kid", kid);
        assertThat(jwk).containsKey("n");
        assertThat(jwk).containsKey("e");
        // e should be AQAB (65537 in base64url)
        assertThat(jwk.get("e")).isEqualTo("AQAB");
    }
}
