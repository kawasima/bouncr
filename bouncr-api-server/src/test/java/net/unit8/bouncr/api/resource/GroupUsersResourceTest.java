package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GroupUsersResource using a real H2 database.
 * Verifies that adding/removing users from groups works correctly via jOOQ.
 */
class GroupUsersResourceTest {
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
    void addUsersToGroup() {
        UserRepository userRepo = new UserRepository(dsl);
        GroupRepository groupRepo = new GroupRepository(dsl);

        // Create test users
        User user1 = userRepo.insert("test1");
        User user2 = userRepo.insert("test2");
        User user3 = userRepo.insert("test3");

        // Create a test group
        Group group = groupRepo.insert(new GroupSpec(new WordName("testgroup"), "Test group"));

        // Add user1 to the group
        groupRepo.addUser(new WordName("testgroup"), user1.id());

        // Verify initial state
        GroupWithUsers groupWithUsers = (GroupWithUsers) groupRepo.findByName("testgroup", true).orElseThrow();
        assertThat(groupWithUsers.users()).hasSize(1);
        assertThat(groupWithUsers.users().getFirst().account()).isEqualTo("test1");

        // Add user2 and user3
        groupRepo.addUser(new WordName("testgroup"), user2.id());
        groupRepo.addUser(new WordName("testgroup"), user3.id());

        // Verify all three users are in the group
        groupWithUsers = (GroupWithUsers) groupRepo.findByName("testgroup", true).orElseThrow();
        assertThat(groupWithUsers.users()).hasSize(3);
        assertThat(groupWithUsers.users().stream().map(User::account))
                .containsExactlyInAnyOrder("test1", "test2", "test3");
    }

    @Test
    void removeUsersFromGroup() {
        UserRepository userRepo = new UserRepository(dsl);
        GroupRepository groupRepo = new GroupRepository(dsl);

        // Create test users
        User user1 = userRepo.insert("test1");
        User user2 = userRepo.insert("test2");
        User user3 = userRepo.insert("test3");

        // Create a group and add all users
        Group group = groupRepo.insert(new GroupSpec(new WordName("testgroup"), "Test group"));
        groupRepo.addUser(new WordName("testgroup"), user1.id());
        groupRepo.addUser(new WordName("testgroup"), user2.id());
        groupRepo.addUser(new WordName("testgroup"), user3.id());

        // Remove user1 and user3
        groupRepo.removeUser(new WordName("testgroup"), user1.id());
        groupRepo.removeUser(new WordName("testgroup"), user3.id());

        // Verify only user2 remains
        GroupWithUsers groupWithUsers = (GroupWithUsers) groupRepo.findByName("testgroup", true).orElseThrow();
        assertThat(groupWithUsers.users()).hasSize(1);
        assertThat(groupWithUsers.users().getFirst().account()).isEqualTo("test2");
    }

    @Test
    void listGroupUsers() {
        UserRepository userRepo = new UserRepository(dsl);
        GroupRepository groupRepo = new GroupRepository(dsl);

        User user1 = userRepo.insert("alpha");
        User user2 = userRepo.insert("beta");
        Group group = groupRepo.insert(new GroupSpec(new WordName("mygroup"), "My group"));
        groupRepo.addUser(new WordName("mygroup"), user1.id());
        groupRepo.addUser(new WordName("mygroup"), user2.id());

        GroupWithUsers loaded = (GroupWithUsers) groupRepo.findByName("mygroup", true).orElseThrow();
        List<User> users = loaded.users();
        assertThat(users).hasSize(2);
        assertThat(users.stream().map(User::account))
                .containsExactlyInAnyOrder("alpha", "beta");
    }
}
