package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfileField;
import net.unit8.bouncr.data.UserProfileValue;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for UserResource operations using a real H2 database.
 * Tests user profile update and verification logic.
 */
class UserResourceTest {
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
    void updateUserProfileWithEmail() {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);

        // The admin user and email/name fields are created by V23 migration
        // Create a new test user
        User user = userRepo.insert("kawasima");

        // Find the email field (created by V23)
        Optional<UserProfileField> emailField = fieldRepo.findByJsonName("email");
        assertThat(emailField).isPresent();
        assertThat(emailField.get().needsVerification()).isTrue();

        // Set initial email
        userRepo.setProfileValue(user.id(), emailField.get().id(), "kawasima0@example.com");

        // Verify the profile value was set
        List<UserProfileValue> values = userRepo.loadProfileValues(user.id());
        assertThat(values).hasSize(1);
        assertThat(values.getFirst().value()).isEqualTo("kawasima0@example.com");
        assertThat(values.getFirst().userProfileField().jsonName()).isEqualTo("email");

        // Update email
        userRepo.setProfileValue(user.id(), emailField.get().id(), "kawasima1@example.com");

        // Verify the profile value was updated
        values = userRepo.loadProfileValues(user.id());
        assertThat(values).hasSize(1);
        assertThat(values.getFirst().value()).isEqualTo("kawasima1@example.com");
    }

    @Test
    void updateUserProfileWithMultipleFields() {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);

        User user = userRepo.insert("testuser");

        Optional<UserProfileField> emailField = fieldRepo.findByJsonName("email");
        Optional<UserProfileField> nameField = fieldRepo.findByJsonName("name");
        assertThat(emailField).isPresent();
        assertThat(nameField).isPresent();

        userRepo.setProfileValue(user.id(), emailField.get().id(), "test@example.com");
        userRepo.setProfileValue(user.id(), nameField.get().id(), "Test User");

        List<UserProfileValue> values = userRepo.loadProfileValues(user.id());
        assertThat(values).hasSize(2);
    }

    @Test
    void findByIdFull() {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);

        User user = userRepo.insert("fulluser");

        Optional<UserProfileField> emailField = fieldRepo.findByJsonName("email");
        userRepo.setProfileValue(user.id(), emailField.get().id(), "full@example.com");

        // Find with full profile data
        User loaded = userRepo.findByIdFull(user.id(), false, false).orElseThrow();
        assertThat(loaded.account()).isEqualTo("fulluser");
        assertThat(loaded.userProfileValues()).hasSize(1);
        assertThat(loaded.userProfileValues().getFirst().value()).isEqualTo("full@example.com");
    }

    @Test
    void findByIdFullWithGroups() {
        UserRepository userRepo = new UserRepository(dsl);

        // The admin user (created by V23) should be in BOUNCR_ADMIN and BOUNCR_USER groups
        User admin = userRepo.findByAccount("admin").orElseThrow();
        User loaded = userRepo.findByIdFull(admin.id(), true, false).orElseThrow();

        assertThat(loaded.groups()).isNotNull();
        assertThat(loaded.groups()).isNotEmpty();
        assertThat(loaded.groups().stream().map(g -> g.name()))
                .contains("BOUNCR_ADMIN", "BOUNCR_USER");
    }
}
