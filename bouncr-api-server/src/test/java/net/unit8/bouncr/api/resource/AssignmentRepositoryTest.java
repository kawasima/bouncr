package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.*;
import net.unit8.bouncr.data.*;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AssignmentRepository with composite keys (group, role, realm).
 * Verifies correct insert/find/delete behavior and cascade semantics on the assignments table.
 */
class AssignmentRepositoryTest {
    private DSLContext dsl;

    private GroupRepository groupRepo;
    private RoleRepository roleRepo;
    private PermissionRepository permRepo;
    private ApplicationRepository appRepo;
    private RealmRepository realmRepo;
    private AssignmentRepository assignmentRepo;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        groupRepo = new GroupRepository(dsl);
        roleRepo = new RoleRepository(dsl);
        permRepo = new PermissionRepository(dsl);
        appRepo = new ApplicationRepository(dsl);
        realmRepo = new RealmRepository(dsl);
        assignmentRepo = new AssignmentRepository(dsl);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    private Group createGroup(String name) {
        return groupRepo.insert(name, name + " description");
    }

    private Role createRole(String name) {
        return roleRepo.insert(name, name + " description");
    }

    private Realm createRealm(String appName, String realmName) {
        Application app = appRepo.findByName(appName, false).orElseGet(
                () -> appRepo.insert(appName, "App " + appName, "/" + appName, "http://localhost", "/"));
        return realmRepo.insert(app.id(), realmName, "http://" + realmName, realmName + " description");
    }

    @Test
    void insertAndFind_byGroupRoleRealm() {
        Group group = createGroup("assign_grp");
        Role role = createRole("assign_role");
        Realm realm = createRealm("assign_app", "assign_realm");

        assignmentRepo.insert(group.id(), role.id(), realm.id());

        Optional<Assignment> found = assignmentRepo.findByGroupRoleRealm("assign_grp", "assign_role", "assign_realm");
        assertThat(found).isPresent();
        assertThat(found.get().group().name()).isEqualTo("assign_grp");
        assertThat(found.get().role().name()).isEqualTo("assign_role");
        assertThat(found.get().realm().name()).isEqualTo("assign_realm");
    }

    @Test
    void exists_returnsTrueForInserted() {
        Group group = createGroup("exist_grp");
        Role role = createRole("exist_role");
        Realm realm = createRealm("exist_app", "exist_realm");

        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isFalse();

        assignmentRepo.insert(group.id(), role.id(), realm.id());
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isTrue();
    }

    @Test
    void insertDuplicate_throwsConstraintViolation() {
        Group group = createGroup("dup_grp");
        Role role = createRole("dup_role");
        Realm realm = createRealm("dup_app", "dup_realm");

        assignmentRepo.insert(group.id(), role.id(), realm.id());
        assertThatThrownBy(() -> assignmentRepo.insert(group.id(), role.id(), realm.id()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void deleteAssignment_removesIt() {
        Group group = createGroup("del_grp");
        Role role = createRole("del_role");
        Realm realm = createRealm("del_app", "del_realm");

        assignmentRepo.insert(group.id(), role.id(), realm.id());
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isTrue();

        assignmentRepo.delete(group.id(), role.id(), realm.id());
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isFalse();
        assertThat(assignmentRepo.findByGroupRoleRealm("del_grp", "del_role", "del_realm")).isEmpty();
    }

    @Test
    void findByGroup_returnsCorrectAssignments() {
        Group group = createGroup("fbg_grp");
        Role role1 = createRole("fbg_role1");
        Role role2 = createRole("fbg_role2");
        Realm realm = createRealm("fbg_app", "fbg_realm");

        assignmentRepo.insert(group.id(), role1.id(), realm.id());
        assignmentRepo.insert(group.id(), role2.id(), realm.id());

        // Create another group with a different assignment to verify isolation
        Group otherGroup = createGroup("fbg_other");
        Role otherRole = createRole("fbg_other_role");
        assignmentRepo.insert(otherGroup.id(), otherRole.id(), realm.id());

        List<Assignment> assignments = assignmentRepo.findByGroup(group.id());
        assertThat(assignments).hasSize(2);
        assertThat(assignments.stream().map(a -> a.role().name()))
                .containsExactlyInAnyOrder("fbg_role1", "fbg_role2");
        // findByGroup returns realm info
        assertThat(assignments.stream().map(a -> a.realm().name()))
                .allMatch("fbg_realm"::equals);
    }

    @Test
    void findByRealm_returnsCorrectAssignments() {
        Group group1 = createGroup("fbr_grp1");
        Group group2 = createGroup("fbr_grp2");
        Role role = createRole("fbr_role");
        Realm realm = createRealm("fbr_app", "fbr_realm");
        Realm otherRealm = createRealm("fbr_app", "fbr_other_realm");

        assignmentRepo.insert(group1.id(), role.id(), realm.id());
        assignmentRepo.insert(group2.id(), role.id(), realm.id());
        assignmentRepo.insert(group1.id(), role.id(), otherRealm.id());

        List<Assignment> assignments = assignmentRepo.findByRealm(realm.id());
        assertThat(assignments).hasSize(2);
        assertThat(assignments.stream().map(a -> a.group().name()))
                .containsExactlyInAnyOrder("fbr_grp1", "fbr_grp2");
    }

    @Test
    void resolveIdByName_returnsCorrectId() {
        Group group = createGroup("resolve_grp");
        Long resolved = assignmentRepo.resolveIdByName("groups", "group_id", "resolve_grp");
        assertThat(resolved).isEqualTo(group.id());
    }

    @Test
    void resolveIdByName_nonExistent_returnsNull() {
        Long resolved = assignmentRepo.resolveIdByName("groups", "group_id", "no_such_group");
        assertThat(resolved).isNull();
    }

    @Test
    void cascadeDeleteGroup_removesAssignments() {
        Group group = createGroup("cascade_grp");
        Role role = createRole("cascade_role");
        Realm realm = createRealm("cascade_app", "cascade_realm");

        assignmentRepo.insert(group.id(), role.id(), realm.id());
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isTrue();

        groupRepo.delete("cascade_grp");
        // Assignment should be gone due to ON DELETE CASCADE on group_id FK
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isFalse();
    }

    @Test
    void cascadeDeleteRole_removesAssignments() {
        Group group = createGroup("cascade_r_grp");
        Role role = createRole("cascade_r_role");
        Realm realm = createRealm("cascade_r_app", "cascade_r_realm");

        assignmentRepo.insert(group.id(), role.id(), realm.id());
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isTrue();

        roleRepo.delete("cascade_r_role");
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isFalse();
    }

    @Test
    void cascadeDeleteRealm_removesAssignments() {
        Group group = createGroup("cascade_re_grp");
        Role role = createRole("cascade_re_role");
        Application app = appRepo.insert("cascade_re_app", "App", "/crapp", "http://a", "/");
        Realm realm = realmRepo.insert(app.id(), "cascade_re_realm", "http://r", "Realm");

        assignmentRepo.insert(group.id(), role.id(), realm.id());
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isTrue();

        realmRepo.delete(app.id(), "cascade_re_realm");
        assertThat(assignmentRepo.exists(group.id(), role.id(), realm.id())).isFalse();
    }

    @Test
    void multipleAssignments_sameGroupDifferentRolesAndRealms() {
        Group group = createGroup("multi_grp");
        Role role1 = createRole("multi_role1");
        Role role2 = createRole("multi_role2");
        Realm realm1 = createRealm("multi_app", "multi_realm1");
        Realm realm2 = createRealm("multi_app", "multi_realm2");

        assignmentRepo.insert(group.id(), role1.id(), realm1.id());
        assignmentRepo.insert(group.id(), role1.id(), realm2.id());
        assignmentRepo.insert(group.id(), role2.id(), realm1.id());

        List<Assignment> byGroup = assignmentRepo.findByGroup(group.id());
        assertThat(byGroup).hasSize(3);

        List<Assignment> byRealm1 = assignmentRepo.findByRealm(realm1.id());
        assertThat(byRealm1).hasSize(2);

        List<Assignment> byRealm2 = assignmentRepo.findByRealm(realm2.id());
        assertThat(byRealm2).hasSize(1);
    }

    @Test
    void findByGroupRoleRealm_nonExistent_returnsEmpty() {
        assertThat(assignmentRepo.findByGroupRoleRealm("nope", "nope", "nope")).isEmpty();
    }

    @Test
    void deleteNonExistent_doesNotThrow() {
        // Deleting with arbitrary IDs should not throw
        assignmentRepo.delete(999999L, 999999L, 999999L);
    }

    @Test
    void userPermissions_resolvedThroughAssignment() {
        // Full chain: user -> membership -> group -> assignment -> role -> role_permission -> permission
        UserRepository userRepo = new UserRepository(dsl);
        RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

        User user = userRepo.insert("perm_user");
        Group group = createGroup("perm_grp");
        Role role = createRole("perm_role");
        Permission perm = permRepo.insert("custom:access", "Custom access");
        rpRepo.addPermission(role.id(), perm.id());

        Application app = appRepo.insert("perm_app", "Perm App", "/perm", "http://a", "/");
        Realm realm = realmRepo.insert(app.id(), "perm_realm", "http://r", "Realm");

        groupRepo.addUser("perm_grp", user.id());
        assignmentRepo.insert(group.id(), role.id(), realm.id());

        // Now verify the user can resolve permissions through the chain
        var permsByRealm = userRepo.getPermissionsByRealm(user.id());
        assertThat(permsByRealm).isNotEmpty();
        List<String> allPerms = permsByRealm.values().stream()
                .flatMap(List::stream)
                .toList();
        assertThat(allPerms).contains("custom:access");
    }
}
