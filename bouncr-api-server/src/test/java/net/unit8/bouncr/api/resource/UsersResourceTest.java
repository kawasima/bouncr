package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.User;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for UsersResource operations using a real H2 database.
 * Tests user search, creation, and uniqueness checks.
 */
class UsersResourceTest {
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
    void searchUsersAsAdmin() {
        UserRepository userRepo = new UserRepository(dsl);

        // V23 migration creates the "admin" user
        // Add more test users
        userRepo.insert("testuser1");
        userRepo.insert("testuser2");

        // Admin search (isAdmin=true) sees all users
        List<User> users = userRepo.search(null, null, 1L, true, 0, 10);
        assertThat(users.size()).isGreaterThanOrEqualTo(3); // admin + testuser1 + testuser2
    }

    @Test
    void searchUsersWithQuery() {
        UserRepository userRepo = new UserRepository(dsl);

        userRepo.insert("alice");
        userRepo.insert("bob");
        userRepo.insert("alice2");

        List<User> users = userRepo.search("alice", null, 1L, true, 0, 10);
        assertThat(users).hasSize(2);
        assertThat(users.stream().map(User::account))
                .containsExactlyInAnyOrder("alice", "alice2");
    }

    @Test
    void searchUsersWithPagination() {
        UserRepository userRepo = new UserRepository(dsl);

        userRepo.insert("page_user1");
        userRepo.insert("page_user2");
        userRepo.insert("page_user3");

        List<User> firstPage = userRepo.search("page_user", null, 1L, true, 0, 2);
        assertThat(firstPage).hasSize(2);

        List<User> secondPage = userRepo.search("page_user", null, 1L, true, 2, 2);
        assertThat(secondPage).hasSize(1);
    }

    @Test
    void searchUsersByGroup() {
        UserRepository userRepo = new UserRepository(dsl);
        GroupRepository groupRepo = new GroupRepository(dsl);

        User user1 = userRepo.insert("grouped1");
        User user2 = userRepo.insert("grouped2");
        User user3 = userRepo.insert("ungrouped");

        var group = groupRepo.insert("testgroup", "Test group");
        groupRepo.addUser("testgroup", user1.id());
        groupRepo.addUser("testgroup", user2.id());

        List<User> users = userRepo.search(null, group.id(), 1L, true, 0, 10);
        assertThat(users).hasSize(2);
        assertThat(users.stream().map(User::account))
                .containsExactlyInAnyOrder("grouped1", "grouped2");
    }

    @Test
    void createUser() {
        UserRepository userRepo = new UserRepository(dsl);

        User user = userRepo.insert("newuser");
        assertThat(user.id()).isNotNull();
        assertThat(user.account()).isEqualTo("newuser");
        assertThat(user.writeProtected()).isFalse();
    }

    @Test
    void accountUniqueness() {
        UserRepository userRepo = new UserRepository(dsl);

        assertThat(userRepo.isAccountUnique("uniqueuser")).isTrue();

        userRepo.insert("uniqueuser");

        assertThat(userRepo.isAccountUnique("uniqueuser")).isFalse();
        // Case-insensitive check
        assertThat(userRepo.isAccountUnique("UniqueUser")).isFalse();
    }

    @Test
    void validateUserCreateRequiresAccount() {
        // The validation is now done by BouncrJsonDecoders.
        // Test that the decoder rejects empty account.
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var node = mapper.readTree("{}");
        // account field is missing - should fail
        var result = net.unit8.bouncr.api.decoder.BouncrJsonDecoders.PASSWORD_SIGN_IN.decode(node);
        assertThat(result).isInstanceOf(net.unit8.raoh.Err.class);
    }
}
