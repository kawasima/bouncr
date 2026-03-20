package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for password credential operations using a real H2 database.
 */
public class PasswordCredentialResourceTest {
    private DSLContext dsl;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void createPasswordCredential() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("test_user");

        String salt = "abcdef1234567890";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);

        userRepo.insertPasswordCredential(user.id(), hash, salt, true);

        // Verify the credential is stored by loading user for sign-in
        User loaded = userRepo.findByAccountForSignIn("test_user").orElseThrow();
        assertThat(loaded.passwordCredential()).isNotNull();
        assertThat(loaded.passwordCredential().salt()).isEqualTo(salt);
        assertThat(loaded.passwordCredential().initial()).isTrue();
        assertThat(loaded.passwordCredential().password()).isEqualTo(hash);
    }

    @Test
    void updatePasswordCredential() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("test_user");

        // Create initial credential
        String salt1 = "saltsaltsaltsalt";
        byte[] hash1 = PasswordUtils.pbkdf2("pass1234", salt1, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash1, salt1, true);

        // Update credential (upsert)
        String salt2 = "newsaltnewsaltne";
        byte[] hash2 = PasswordUtils.pbkdf2("pass5678", salt2, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash2, salt2, false);

        // Verify the credential is updated
        User loaded = userRepo.findByAccountForSignIn("test_user").orElseThrow();
        assertThat(loaded.passwordCredential()).isNotNull();
        assertThat(loaded.passwordCredential().salt()).isEqualTo(salt2);
        assertThat(loaded.passwordCredential().initial()).isFalse();
        assertThat(loaded.passwordCredential().password()).isEqualTo(hash2);
    }

    @Test
    void verifyPasswordMatch() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("test_user");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        User loaded = userRepo.findByAccountForSignIn("test_user").orElseThrow();
        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), 600_000);
        assertThat(loaded.passwordCredential().password()).isEqualTo(checkHash);
    }

    @Test
    void verifyPasswordMismatch() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("test_user");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        User loaded = userRepo.findByAccountForSignIn("test_user").orElseThrow();
        byte[] wrongHash = PasswordUtils.pbkdf2("wrongpassword", loaded.passwordCredential().salt(), 600_000);
        assertThat(loaded.passwordCredential().password()).isNotEqualTo(wrongHash);
    }

    @Test
    void deletePasswordCredential() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("test_user");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        // Delete
        userRepo.deletePasswordCredential(user.id());

        // Verify it's gone
        User loaded = userRepo.findByAccountForSignIn("test_user").orElseThrow();
        assertThat(loaded.passwordCredential()).isNull();
    }
}
