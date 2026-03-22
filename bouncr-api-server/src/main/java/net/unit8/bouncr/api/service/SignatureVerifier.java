package net.unit8.bouncr.api.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * Verifies HMAC-SHA256 request signatures in the format used by
 * {@code X-Bouncr-Signature: t={timestamp},v1={hex-hmac}}.
 *
 * <p>The signature is computed over {@code "{timestamp}.{payload}"} where
 * payload is the canonical representation of the signed content (e.g. session_id).
 * Timestamp tolerance prevents replay attacks.
 */
public class SignatureVerifier {
    private static final long TOLERANCE_SECONDS = 30;
    private final byte[] key;

    public SignatureVerifier(String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies that the signature header value matches the expected HMAC
     * for the given payload, and that the timestamp is within tolerance.
     *
     * @param signatureHeader header value in format "t=...,v1=..."
     * @param payload the canonical content that was signed
     * @return true if the signature is valid and the timestamp is fresh
     */
    public boolean verify(String signatureHeader, String payload) {
        if (signatureHeader == null) return false;

        String timestamp = null;
        String signature = null;
        for (String part : signatureHeader.split(",")) {
            if (part.startsWith("t=")) timestamp = part.substring(2);
            else if (part.startsWith("v1=")) signature = part.substring(3);
        }
        if (timestamp == null || signature == null) return false;

        long t;
        try {
            t = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - t) > TOLERANCE_SECONDS) return false;

        String signed = timestamp + "." + payload;
        byte[] expected = hmacSha256(signed);
        byte[] provided = hexDecode(signature);
        if (provided == null) return false;

        return MessageDigest.isEqual(expected, provided);
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HmacSHA256 unavailable", e);
        }
    }

    private static byte[] hexDecode(String hex) {
        if (hex.length() % 2 != 0) return null;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi == -1 || lo == -1) return null;
            bytes[i] = (byte) ((hi << 4) | lo);
        }
        return bytes;
    }
}
