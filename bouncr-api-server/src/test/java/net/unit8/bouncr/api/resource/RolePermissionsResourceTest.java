package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.PermissionRepository;
import net.unit8.bouncr.api.repository.RolePermissionRepository;
import net.unit8.bouncr.api.repository.RoleRepository;
import net.unit8.bouncr.data.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RolePermissions operations using a real H2 database.
 * Verifies adding/removing permissions to/from roles via jOOQ.
 */
class RolePermissionsResourceTest {
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
    void addPermissionsToRole() {
        RoleRepository roleRepo = new RoleRepository(dsl);
        PermissionRepository permRepo = new PermissionRepository(dsl);
        RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

        // Create a role and permissions (some already exist from V23 migration)
        Role role = roleRepo.insert(new RoleSpec(new WordName("test_role"), "Test role"));
        Permission perm1 = permRepo.insert(new PermissionSpec(new PermissionName("custom:read"), "Custom read permission"));
        Permission perm2 = permRepo.insert(new PermissionSpec(new PermissionName("custom:write"), "Custom write permission"));
        Permission perm3 = permRepo.insert(new PermissionSpec(new PermissionName("custom:delete"), "Custom delete permission"));

        // Add perm1 and perm2
        rpRepo.addPermission(role.id(), perm1.id());
        rpRepo.addPermission(role.id(), perm2.id());

        List<Permission> permissions = rpRepo.findPermissionsByRole("test_role");
        assertThat(permissions).hasSize(2);
        assertThat(permissions.stream().map(p -> p.name().value()))
                .containsExactlyInAnyOrder("custom:read", "custom:write");

        // Add perm3
        rpRepo.addPermission(role.id(), perm3.id());

        permissions = rpRepo.findPermissionsByRole("test_role");
        assertThat(permissions).hasSize(3);
        assertThat(permissions.stream().map(p -> p.name().value()))
                .containsExactlyInAnyOrder("custom:read", "custom:write", "custom:delete");
    }

    @Test
    void removePermissionsFromRole() {
        RoleRepository roleRepo = new RoleRepository(dsl);
        PermissionRepository permRepo = new PermissionRepository(dsl);
        RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

        Role role = roleRepo.insert(new RoleSpec(new WordName("test_role"), "Test role"));
        Permission perm1 = permRepo.insert(new PermissionSpec(new PermissionName("custom:read"), "Custom read"));
        Permission perm2 = permRepo.insert(new PermissionSpec(new PermissionName("custom:write"), "Custom write"));
        Permission perm3 = permRepo.insert(new PermissionSpec(new PermissionName("custom:delete"), "Custom delete"));

        rpRepo.addPermission(role.id(), perm1.id());
        rpRepo.addPermission(role.id(), perm2.id());
        rpRepo.addPermission(role.id(), perm3.id());

        // Remove perm1 and perm3
        rpRepo.removePermission(role.id(), perm1.id());
        rpRepo.removePermission(role.id(), perm3.id());

        // Only perm2 should remain
        List<Permission> permissions = rpRepo.findPermissionsByRole("test_role");
        assertThat(permissions).hasSize(1);
        assertThat(permissions.getFirst().name().value()).isEqualTo("custom:write");
    }

    @Test
    void replacePermissionsOnRole() {
        RoleRepository roleRepo = new RoleRepository(dsl);
        PermissionRepository permRepo = new PermissionRepository(dsl);
        RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

        Role role = roleRepo.insert(new RoleSpec(new WordName("test_role"), "Test role"));
        Permission perm1 = permRepo.insert(new PermissionSpec(new PermissionName("custom:read"), "Custom read"));
        Permission perm2 = permRepo.insert(new PermissionSpec(new PermissionName("custom:write"), "Custom write"));
        Permission perm3 = permRepo.insert(new PermissionSpec(new PermissionName("custom:delete"), "Custom delete"));

        rpRepo.addPermission(role.id(), perm1.id());
        rpRepo.addPermission(role.id(), perm2.id());

        // Replace with perm2 and perm3
        rpRepo.replacePermissions(role.id(), List.of(perm2.id(), perm3.id()));

        List<Permission> permissions = rpRepo.findPermissionsByRole("test_role");
        assertThat(permissions).hasSize(2);
        assertThat(permissions.stream().map(p -> p.name().value()))
                .containsExactlyInAnyOrder("custom:write", "custom:delete");
    }
}
