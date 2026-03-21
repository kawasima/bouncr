package net.unit8.bouncr.util;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(encryptor.encrypt(data)).isEqualTo(data);
        assertThat(encryptor.decrypt(data)).isEqualTo(data);
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
}
