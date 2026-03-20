package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for password sign-in logic using a real H2 database.
 * Tests the authentication flow: load user with credentials, verify password.
 */
public class PasswordSignInResourceTest {
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
    void authenticationSuccessful() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        // Simulate authentication: load user and verify password
        User loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        assertThat(loaded.passwordCredential()).isNotNull();

        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), 600_000);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), checkHash)).isTrue();
    }

    @Test
    void authenticationFailedWrongPassword() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        User loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        byte[] wrongHash = PasswordUtils.pbkdf2("wrongpass", loaded.passwordCredential().salt(), 600_000);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), wrongHash)).isFalse();
    }

    @Test
    void authenticationFailedUserNotFound() {
        UserRepository userRepo = new UserRepository(dsl);
        assertThat(userRepo.findByAccountForSignIn("nonexistent")).isEmpty();
    }

    @Test
    void authenticationWithInitialPassword() {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.insert("kawasima");

        String salt = "saltsaltsaltsalt";
        byte[] hash = PasswordUtils.pbkdf2("pass1234", salt, 600_000);
        userRepo.insertPasswordCredential(user.id(), hash, salt, true);

        User loaded = userRepo.findByAccountForSignIn("kawasima").orElseThrow();
        assertThat(loaded.passwordCredential().initial()).isTrue();

        // Password still matches even though it's initial
        byte[] checkHash = PasswordUtils.pbkdf2("pass1234", loaded.passwordCredential().salt(), 600_000);
        assertThat(Arrays.equals(loaded.passwordCredential().password(), checkHash)).isTrue();
    }
}
