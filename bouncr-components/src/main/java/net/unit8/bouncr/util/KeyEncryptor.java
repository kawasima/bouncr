package net.unit8.bouncr.util;

import enkan.exception.MisconfigurationException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * AES-256-GCM encryption/decryption for sensitive data at rest (e.g., OIDC private keys).
 *
 * <p>Format: [12-byte IV][ciphertext + 16-byte GCM tag]</p>
 *
 * <p>When no encryption key is configured (dev mode), {@link #encrypt} and {@link #decrypt}
 * return the input unchanged.</p>
 */
public class KeyEncryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom random;

    /**
     * Create an encryptor with the given 32-byte AES key, or null for no-op (dev mode).
     */
    public KeyEncryptor(byte[] keyBytes, SecureRandom random) {
        if (keyBytes != null && keyBytes.length != 32) {
            throw new MisconfigurationException("bouncr.KEY_ENCRYPTION_KEY_LENGTH",
                    "Key encryption key must be 32 bytes (AES-256), got " + keyBytes.length);
        }
        this.secretKey = keyBytes != null ? new SecretKeySpec(keyBytes, "AES") : null;
        this.random = random;
    }

    public byte[] encrypt(byte[] plaintext) {
        if (secretKey == null) return plaintext;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Prepend IV to ciphertext
            return ByteBuffer.allocate(IV_LENGTH + ciphertext.length)
                    .put(iv).put(ciphertext).array();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt", e);
        }
    }

    public byte[] decrypt(byte[] data) {
        if (secretKey == null) return data;
        try {
            if (data.length < IV_LENGTH + 16) {
                // Too short to be encrypted data (IV + GCM tag minimum) — treat as plaintext
                return data;
            }
            ByteBuffer buf = ByteBuffer.wrap(data);
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            // Decryption failed — data may be legacy plaintext (stored before encryption was enabled)
            return data;
        }
    }

    /**
     * Returns true if encryption is enabled (key is configured).
     */
    public boolean isEnabled() {
        return secretKey != null;
    }
}
