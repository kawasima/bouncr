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
 * Integration tests for user operations using a real H2 database with Flyway migrations.
 * Verifies that the admin user and initial data from V23 migration are present.
 */
class UsersResourceWithDbTest {
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
    void adminUserExistsAfterMigration() {
        UserRepository userRepo = new UserRepository(dsl);

        User admin = userRepo.findByAccount("admin").orElseThrow();
        assertThat(admin.account()).isEqualTo("admin");
        assertThat(admin.writeProtected()).isTrue();
    }

    @Test
    void adminUserHasProfileValues() {
        UserRepository userRepo = new UserRepository(dsl);

        User admin = userRepo.findByAccount("admin").orElseThrow();
        User full = userRepo.findByIdFull(admin.id(), false, false).orElseThrow();
        assertThat(full.userProfileValues()).isNotEmpty();
        assertThat(full.userProfileValues().stream()
                .filter(v -> "email".equals(v.userProfileField().jsonName()))
                .findFirst()
                .orElseThrow()
                .value()).isEqualTo("admin@example.com");
    }

    @Test
    void adminUserHasGroups() {
        UserRepository userRepo = new UserRepository(dsl);

        User admin = userRepo.findByAccount("admin").orElseThrow();
        User full = userRepo.findByIdFull(admin.id(), true, false).orElseThrow();
        assertThat(full.groups()).isNotEmpty();
        assertThat(full.groups().stream().map(g -> g.name()))
                .contains("BOUNCR_ADMIN", "BOUNCR_USER");
    }

    @Test
    void adminUserHasPermissions() {
        UserRepository userRepo = new UserRepository(dsl);

        User admin = userRepo.findByAccount("admin").orElseThrow();
        var permissionsByRealm = userRepo.getPermissionsByRealm(admin.id());
        assertThat(permissionsByRealm).isNotEmpty();
        // Admin should have admin permissions from V23 migration
        List<String> allPermissions = permissionsByRealm.values().stream()
                .flatMap(List::stream)
                .toList();
        assertThat(allPermissions).contains("any_user:read", "any_user:create");
    }

    @Test
    void searchUsersInSameGroup() {
        UserRepository userRepo = new UserRepository(dsl);
        GroupRepository groupRepo = new GroupRepository(dsl);

        User user1 = userRepo.insert("member1");
        User user2 = userRepo.insert("member2");
        User user3 = userRepo.insert("outsider");

        var group = groupRepo.insert("shared_group", "A shared group");
        groupRepo.addUser("shared_group", user1.id());
        groupRepo.addUser("shared_group", user2.id());

        // Non-admin search: user1 can only see users in same groups
        List<User> visible = userRepo.search(null, null, user1.id(), false, 0, 100);
        assertThat(visible.stream().map(User::account))
                .contains("member2")
                .doesNotContain("outsider");
    }

    @Test
    void deleteUser() {
        UserRepository userRepo = new UserRepository(dsl);

        User user = userRepo.insert("deleteme");
        assertThat(userRepo.findByAccount("deleteme")).isPresent();

        userRepo.delete(user.id());
        assertThat(userRepo.findByAccount("deleteme")).isEmpty();
    }
}
