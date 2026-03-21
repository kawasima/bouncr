package net.unit8.bouncr.sign;

import tools.jackson.databind.json.JsonMapper;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * RS256 (RSASSA-PKCS1-v1_5 + SHA-256) JWT signer for OIDC IdP token issuance.
 */
public class RsaJwtSigner {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    /**
     * Sign a JWT with RS256 using the given private key bytes (PKCS#8 encoded).
     */
    public static String sign(Map<String, Object> payload, byte[] privateKeyBytes) {
        return doSign(Map.of("alg", "RS256", "typ", "JWT"), payload, privateKeyBytes);
    }

    /**
     * Sign a JWT with RS256 using the given private key bytes, including a kid in the header.
     */
    public static String sign(Map<String, Object> payload, byte[] privateKeyBytes, String kid) {
        return doSign(Map.of("alg", "RS256", "typ", "JWT", "kid", kid), payload, privateKeyBytes);
    }

    private static String doSign(Map<String, Object> header, Map<String, Object> payload, byte[] privateKeyBytes) {
        try {
            String headerB64 = BASE64URL.encodeToString(JSON.writeValueAsBytes(header));
            String payloadB64 = BASE64URL.encodeToString(JSON.writeValueAsBytes(payload));
            String signingInput = headerB64 + "." + payloadB64;

            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(signingInput.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signature = BASE64URL.encodeToString(sig.sign());

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    /**
     * Derive a kid (Key ID) from an RSA public key (SHA-256 thumbprint, first 16 base64url chars).
     */
    public static String deriveKid(byte[] publicKeyBytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKeyBytes);
            return BASE64URL.encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive kid", e);
        }
    }

    /**
     * Convert X.509 encoded public key bytes to JWK components (n, e).
     */
    public static Map<String, String> publicKeyToJwk(byte[] publicKeyBytes, String kid) {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            java.security.interfaces.RSAPublicKey rsaKey = (java.security.interfaces.RSAPublicKey) publicKey;
            return Map.of(
                    "kty", "RSA",
                    "use", "sig",
                    "alg", "RS256",
                    "kid", kid,
                    "n", BASE64URL.encodeToString(toUnsignedBytes(rsaKey.getModulus())),
                    "e", BASE64URL.encodeToString(toUnsignedBytes(rsaKey.getPublicExponent()))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert public key to JWK", e);
        }
    }

    /**
     * Convert BigInteger to unsigned byte array (strip leading zero if present).
     */
    private static byte[] toUnsignedBytes(java.math.BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }
}
