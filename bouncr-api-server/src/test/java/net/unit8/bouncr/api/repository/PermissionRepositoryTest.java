package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.api.resource.MockFactory;
import net.unit8.bouncr.data.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PermissionRepository using H2 in-memory database with Flyway migrations.
 */
class PermissionRepositoryTest {
    private DSLContext dsl;
    private PermissionRepository repo;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        repo = new PermissionRepository(dsl);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void insertAndFindByName() {
        Permission perm = repo.insert(new PermissionSpec(new PermissionName("custom:action"), "A custom action permission"));

        assertThat(perm.id()).isNotNull();
        assertThat(perm.name().value()).isEqualTo("custom:action");
        assertThat(perm.description()).isEqualTo("A custom action permission");
        assertThat(perm.writeProtected()).isFalse();

        Optional<Permission> found = repo.findByName("custom:action");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(perm.id());
        assertThat(found.get().name().value()).isEqualTo("custom:action");
    }

    @Test
    void findByNameNotFound() {
        Optional<Permission> found = repo.findByName("nonexistent:permission");
        assertThat(found).isEmpty();
    }

    @Test
    void searchWithQuery() {
        // V23 migration inserts many permissions (any_user:read, any_user:create, etc.)
        // Insert a custom one
        repo.insert(new PermissionSpec(new PermissionName("custom:search"), "Searchable permission"));

        List<Permission> results = repo.search("custom", null, true, 0, 100);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name().value()).isEqualTo("custom:search");
    }

    @Test
    void searchWithoutQuery() {
        // Should return all permissions (including those from V23 migration)
        List<Permission> results = repo.search(null, null, true, 0, 100);
        assertThat(results.size()).isGreaterThanOrEqualTo(30); // V23 inserts many admin permissions
    }

    @Test
    void searchWithPagination() {
        List<Permission> firstPage = repo.search(null, null, true, 0, 5);
        assertThat(firstPage).hasSize(5);

        List<Permission> secondPage = repo.search(null, null, true, 5, 5);
        assertThat(secondPage).hasSize(5);

        // Pages should not overlap
        assertThat(firstPage.stream().map(Permission::id).toList())
                .doesNotContainAnyElementsOf(secondPage.stream().map(Permission::id).toList());
    }

    @Test
    void isNameUnique() {
        assertThat(repo.isNameUnique(new PermissionName("unique:perm"))).isTrue();

        repo.insert(new PermissionSpec(new PermissionName("unique:perm"), "Unique permission"));

        assertThat(repo.isNameUnique(new PermissionName("unique:perm"))).isFalse();
        // Case-insensitive uniqueness
        assertThat(repo.isNameUnique(new PermissionName("Unique:Perm"))).isFalse();
    }

    @Test
    void updatePermission() {
        repo.insert(new PermissionSpec(new PermissionName("old:name"), "Old description"));

        repo.update(new PermissionName("old:name"), new PermissionSpec(new PermissionName("new:name"), "New description"));

        assertThat(repo.findByName("old:name")).isEmpty();

        Optional<Permission> updated = repo.findByName("new:name");
        assertThat(updated).isPresent();
        assertThat(updated.get().description()).isEqualTo("New description");
    }

    @Test
    void updatePermissionNameOnly() {
        repo.insert(new PermissionSpec(new PermissionName("rename:me"), "Keep this description"));

        repo.update(new PermissionName("rename:me"), new PermissionSpec(new PermissionName("renamed:me"), null));

        Optional<Permission> updated = repo.findByName("renamed:me");
        assertThat(updated).isPresent();
        assertThat(updated.get().description()).isEqualTo("Keep this description");
    }

    @Test
    void updatePermissionDescriptionOnly() {
        repo.insert(new PermissionSpec(new PermissionName("keep:name"), "Old description"));

        repo.update(new PermissionName("keep:name"), new PermissionSpec(null, "Updated description"));

        Optional<Permission> updated = repo.findByName("keep:name");
        assertThat(updated).isPresent();
        assertThat(updated.get().description()).isEqualTo("Updated description");
    }

    @Test
    void deletePermission() {
        repo.insert(new PermissionSpec(new PermissionName("delete:me"), "To be deleted"));
        assertThat(repo.findByName("delete:me")).isPresent();

        repo.delete(new PermissionName("delete:me"));
        assertThat(repo.findByName("delete:me")).isEmpty();
    }

    @Test
    void searchNonAdminUserSeesOnlyAssignedPermissions() {
        // Create a user, group, role, and assign permissions
        UserRepository userRepo = new UserRepository(dsl);
        GroupRepository groupRepo = new GroupRepository(dsl);
        RoleRepository roleRepo = new RoleRepository(dsl);
        RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

        var user = userRepo.insert("limited_user");
        var group = groupRepo.insert(new GroupSpec(new WordName("limited_group"), "Limited group"));
        var role = roleRepo.insert(new RoleSpec(new WordName("limited_role"), "Limited role"));

        Permission customPerm = repo.insert(new PermissionSpec(new PermissionName("limited:read"), "Limited read"));
        rpRepo.addPermission(role.id(), customPerm.id());

        // Add user to group
        groupRepo.addUser(new WordName("limited_group"), user.id());

        // Assign role to group in realm (use the BOUNCR realm from V23)
        var realmId = dsl.select(org.jooq.impl.DSL.field("realm_id", Long.class))
                .from(org.jooq.impl.DSL.table("realms"))
                .where(org.jooq.impl.DSL.field("name").eq("BOUNCR"))
                .fetchOne(org.jooq.impl.DSL.field("realm_id", Long.class));

        dsl.insertInto(org.jooq.impl.DSL.table("assignments"),
                        org.jooq.impl.DSL.field("group_id"),
                        org.jooq.impl.DSL.field("role_id"),
                        org.jooq.impl.DSL.field("realm_id"))
                .values(group.id(), role.id(), realmId)
                .execute();

        // Non-admin search: should only see permissions assigned to this user
        List<Permission> visible = repo.search(null, user.id(), false, 0, 100);
        assertThat(visible.stream().map(p -> p.name().value())).contains("limited:read");

        // The user should NOT see permissions not assigned to them
        repo.insert(new PermissionSpec(new PermissionName("other:perm"), "Other permission"));
        visible = repo.search(null, user.id(), false, 0, 100);
        assertThat(visible.stream().map(p -> p.name().value())).doesNotContain("other:perm");
    }
}
