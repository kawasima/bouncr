package net.unit8.bouncr.util;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyEncryptorTest {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void encryptDecrypt_roundtrip() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        KeyEncryptor encryptor = new KeyEncryptor(key, RANDOM);

        byte[] plaintext = "Hello, World! This is a private key.".getBytes();
        byte[] encrypted = encryptor.encrypt(plaintext);
        byte[] decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
        // Encrypted data should be larger (IV + tag overhead)
        assertThat(encrypted.length).isGreaterThan(plaintext.length);
    }

    @Test
    void noOpMode_whenKeyIsNull() {
        KeyEncryptor encryptor = new KeyEncryptor(null, RANDOM);
        byte[] data = "plaintext data".getBytes();

        byte[] encrypted = encryptor.encrypt(data);
        byte[] decrypted = encryptor.decrypt(data);
        assertThat(encrypted).isEqualTo(data);
        assertThat(decrypted).isEqualTo(data);
        assertThat(encrypted).isNotSameAs(data);
        assertThat(decrypted).isNotSameAs(data);
        assertThat(encryptor.isEnabled()).isFalse();
    }

    @Test
    void differentIVs_produceDifferentCiphertexts() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        KeyEncryptor encryptor = new KeyEncryptor(key, RANDOM);

        byte[] plaintext = "same data".getBytes();
        byte[] enc1 = encryptor.encrypt(plaintext);
        byte[] enc2 = encryptor.encrypt(plaintext);

        // Same plaintext should produce different ciphertexts (different IV)
        assertThat(enc1).isNotEqualTo(enc2);
        // But both should decrypt to the same plaintext
        assertThat(encryptor.decrypt(enc1)).isEqualTo(plaintext);
        assertThat(encryptor.decrypt(enc2)).isEqualTo(plaintext);
    }

    @Test
    void isEnabled_trueWhenKeyProvided() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        KeyEncryptor encryptor = new KeyEncryptor(key, RANDOM);
        assertThat(encryptor.isEnabled()).isTrue();
    }

    @Test
    void encryptedData_hasMagicPrefix() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        KeyEncryptor encryptor = new KeyEncryptor(key, RANDOM);

        byte[] encrypted = encryptor.encrypt("test".getBytes());
        // Magic prefix: ENC\x01
        assertThat(encrypted[0]).isEqualTo((byte) 'E');
        assertThat(encrypted[1]).isEqualTo((byte) 'N');
        assertThat(encrypted[2]).isEqualTo((byte) 'C');
        assertThat(encrypted[3]).isEqualTo((byte) 1);
    }

    @Test
    void decrypt_legacyPlaintextWithoutMagic_returnedAsIs() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        KeyEncryptor encryptor = new KeyEncryptor(key, RANDOM);

        // Data without magic prefix is treated as legacy plaintext
        byte[] legacyData = "plaintext-private-key-bytes".getBytes();
        byte[] result = encryptor.decrypt(legacyData);
        assertThat(result).isEqualTo(legacyData);
        assertThat(result).isNotSameAs(legacyData);
    }

    @Test
    void decrypt_corruptedEncryptedData_throwsException() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        KeyEncryptor encryptor = new KeyEncryptor(key, RANDOM);

        // Data with magic prefix but corrupted content
        byte[] corrupted = new byte[]{'E', 'N', 'C', 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertThatThrownBy(() -> encryptor.decrypt(corrupted))
                .isInstanceOf(enkan.exception.MisconfigurationException.class);
    }

    @Test
    void invalidKeyLength_throwsMisconfigurationException() {
        byte[] shortKey = new byte[16]; // AES-128, not AES-256
        assertThatThrownBy(() -> new KeyEncryptor(shortKey, RANDOM))
                .isInstanceOf(enkan.exception.MisconfigurationException.class);
    }
}
