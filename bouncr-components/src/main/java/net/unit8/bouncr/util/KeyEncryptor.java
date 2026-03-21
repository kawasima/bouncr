package net.unit8.bouncr.util;

import enkan.exception.MisconfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(KeyEncryptor.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    // Magic prefix to distinguish encrypted data from legacy plaintext
    private static final byte[] MAGIC = {'E', 'N', 'C', 1};

    private final SecretKey secretKey;
    private final SecureRandom random;

    /**
     * Create an encryptor with the given 32-byte AES key, or null for no-op (dev mode).
     */
    public KeyEncryptor(byte[] keyBytes, SecureRandom random) {
        if (keyBytes != null && keyBytes.length != 32) {
            throw new MisconfigurationException("bouncr.INVALID_KEY_ENCRYPTION_KEY", keyBytes.length);
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

            // Format: [MAGIC][IV][ciphertext + GCM tag]
            return ByteBuffer.allocate(MAGIC.length + IV_LENGTH + ciphertext.length)
                    .put(MAGIC).put(iv).put(ciphertext).array();
        } catch (Exception e) {
            throw new MisconfigurationException("bouncr.ENCRYPTION_FAILED", e.getMessage(), e);
        }
    }

    public byte[] decrypt(byte[] data) {
        if (secretKey == null) return data;

        // Check magic prefix — if absent, data is legacy plaintext
        if (!hasMagicPrefix(data)) {
            LOG.warn("Data lacks encryption magic prefix — treating as legacy plaintext");
            return data;
        }

        try {
            ByteBuffer buf = ByteBuffer.wrap(data, MAGIC.length, data.length - MAGIC.length);
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            // Magic prefix present but decryption failed — this is a real error (wrong key, corruption)
            throw new MisconfigurationException("bouncr.DECRYPTION_FAILED", e.getMessage(), e);
        }
    }

    private boolean hasMagicPrefix(byte[] data) {
        if (data.length < MAGIC.length) return false;
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) return false;
        }
        return true;
    }

    /**
     * Returns true if encryption is enabled (key is configured).
     */
    public boolean isEnabled() {
        return secretKey != null;
    }
}
