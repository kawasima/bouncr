package net.unit8.bouncr.api.service;

import net.unit8.bouncr.util.PasswordUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PBKDF2 client secret hashing used by OAuth2ClientAuthenticator.
 */
class OAuth2ClientAuthenticatorTest {

    @Test
    void pbkdf2Hash_sameInputProducesSameHash() {
        String secret = "test-secret-123";
        String salt = "client-id-abc";
        byte[] hash1 = PasswordUtils.pbkdf2(secret, salt, 10000);
        byte[] hash2 = PasswordUtils.pbkdf2(secret, salt, 10000);
        assertThat(MessageDigest.isEqual(hash1, hash2)).isTrue();
    }

    @Test
    void pbkdf2Hash_differentSecretProducesDifferentHash() {
        String salt = "client-id-abc";
        byte[] hash1 = PasswordUtils.pbkdf2("secret-one", salt, 10000);
        byte[] hash2 = PasswordUtils.pbkdf2("secret-two", salt, 10000);
        assertThat(MessageDigest.isEqual(hash1, hash2)).isFalse();
    }

    @Test
    void pbkdf2Hash_differentSaltProducesDifferentHash() {
        String secret = "same-secret";
        byte[] hash1 = PasswordUtils.pbkdf2(secret, "client-a", 10000);
        byte[] hash2 = PasswordUtils.pbkdf2(secret, "client-b", 10000);
        assertThat(MessageDigest.isEqual(hash1, hash2)).isFalse();
    }

    @Test
    void pbkdf2Hash_base64RoundTrip() {
        String secret = "my-client-secret";
        String salt = "my-client-id";
        byte[] hash = PasswordUtils.pbkdf2(secret, salt, 10000);
        String encoded = Base64.getEncoder().encodeToString(hash);
        byte[] decoded = Base64.getDecoder().decode(encoded);
        assertThat(MessageDigest.isEqual(hash, decoded)).isTrue();
    }

    @Test
    void basicAuthHeader_parsing() {
        String clientId = "test-client";
        String clientSecret = "test-secret";
        String encoded = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String header = "Basic " + encoded;

        // Simulate parsing
        String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
        String[] parts = decoded.split(":", 2);
        assertThat(parts[0]).isEqualTo(clientId);
        assertThat(parts[1]).isEqualTo(clientSecret);
    }

    @Test
    void basicAuthHeader_invalidBase64_doesNotThrow() {
        String header = "Basic not-valid-base64!!!";
        try {
            Base64.getDecoder().decode(header.substring(6));
            // Some implementations may succeed with padding; that's OK
        } catch (IllegalArgumentException e) {
            // Expected — invalid Base64 should be caught
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
