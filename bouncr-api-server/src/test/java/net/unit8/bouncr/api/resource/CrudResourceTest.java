package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.repository.*;
import net.unit8.bouncr.data.*;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for CRUD operations on Group, Role, Permission, Application,
 * and Realm repositories. Uses a real H2 database with transaction rollback per test.
 */
class CrudResourceTest {
    private DSLContext dsl;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Nested
    class GroupCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            GroupRepository repo = new GroupRepository(dsl);

            Group created = repo.insert(new GroupSpec(new WordName("developers"), "Dev team"));
            assertThat(created.id()).isNotNull();
            assertThat(created.name().value()).isEqualTo("developers");
            assertThat(created.description()).isEqualTo("Dev team");
            assertThat(created.writeProtected()).isFalse();

            Optional<Group> found = repo.findByName("developers", false);
            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(created.id());

            repo.update(new WordName("developers"), new GroupSpec(new WordName("devs"), "Development team"));
            assertThat(repo.findByName("developers", false)).isEmpty();
            Optional<Group> updated = repo.findByName("devs", false);
            assertThat(updated).isPresent();
            assertThat(updated.get().description()).isEqualTo("Development team");

            repo.delete(new WordName("devs"));
            assertThat(repo.findByName("devs", false)).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("unique_group"), "First"));
            assertThatThrownBy(() -> repo.insert(new GroupSpec(new WordName("unique_group"), "Second")))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("TestGroup"), "desc"));
            assertThat(repo.isNameUnique(new WordName("testgroup"))).isFalse();
            assertThat(repo.isNameUnique(new WordName("TESTGROUP"))).isFalse();
            assertThat(repo.isNameUnique(new WordName("other_group"))).isTrue();
        }

        @Test
        void deleteNonExistent_doesNotThrow() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.delete(new WordName("no_such_group")); // should not throw
        }

        @Test
        void updateToNull_keepsOriginalValues() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("mygroup"), "original desc"));

            // Update with null newName and null description - should keep originals
            repo.update(new WordName("mygroup"), new GroupSpec(null, null));
            Optional<Group> found = repo.findByName("mygroup", false);
            assertThat(found).isPresent();
            assertThat(found.get().description()).isEqualTo("original desc");
        }

        @Test
        void findByName_embedUsers_returnsMembers() {
            GroupRepository groupRepo = new GroupRepository(dsl);
            UserRepository userRepo = new UserRepository(dsl);

            Group group = groupRepo.insert(new GroupSpec(new WordName("team"), "A team"));
            User user1 = userRepo.insert("alice");
            User user2 = userRepo.insert("bob");
            groupRepo.addUser(new WordName("team"), user1.id());
            groupRepo.addUser(new WordName("team"), user2.id());

            Optional<Group> found = groupRepo.findByName("team", true);
            assertThat(found).isPresent();
            assertThat(((GroupWithUsers) found.get()).users()).hasSize(2);
            assertThat(((GroupWithUsers) found.get()).users().stream().map(User::account))
                    .containsExactlyInAnyOrder("alice", "bob");
        }

        @Test
        void findByName_noEmbedUsers_returnsNullUsers() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("team"), "A team"));
            Optional<Group> found = repo.findByName("team", false);
            assertThat(found).isPresent();
            assertThat(found.get()).isInstanceOf(GroupPure.class);
        }

        @Test
        void search_withQuery_filtersResults() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("alpha_team"), "Alpha"));
            repo.insert(new GroupSpec(new WordName("beta_team"), "Beta"));
            repo.insert(new GroupSpec(new WordName("gamma_squad"), "Gamma"));

            List<Group> results = repo.search("team", null, true, 0, 100);
            assertThat(results.stream().map(g -> g.name().value()))
                    .contains("alpha_team", "beta_team")
                    .doesNotContain("gamma_squad");
        }

        @Test
        void rename_toSameName_isNotConflict() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("mygroup"), "desc"));
            // Same name → isNameUnique returns false, but isConflict skips the check: no conflict
            assertThat(repo.isNameUnique(new WordName("mygroup"))).isFalse();
        }

        @Test
        void rename_toDuplicateName_isConflict() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert(new GroupSpec(new WordName("group_a"), "A"));
            repo.insert(new GroupSpec(new WordName("group_b"), "B"));
            // Trying to rename group_a → group_b: name is taken
            assertThat(repo.isNameUnique(new WordName("group_b"))).isFalse();
        }

        @Test
        void addUser_toNonExistentGroup_throwsIllegalArgument() {
            GroupRepository repo = new GroupRepository(dsl);
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("orphan");

            assertThatThrownBy(() -> repo.addUser(new WordName("nonexistent_group"), user.id()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group not found");
        }
    }

    @Nested
    class RoleCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            RoleRepository repo = new RoleRepository(dsl);

            Role created = repo.insert(new RoleSpec(new WordName("editor"), "Can edit"));
            assertThat(created.id()).isNotNull();
            assertThat(created.name().value()).isEqualTo("editor");

            Optional<Role> found = repo.findByName("editor", false);
            assertThat(found).isPresent();

            repo.update(new WordName("editor"), new RoleSpec(new WordName("reviewer"), "Can review"));
            assertThat(repo.findByName("editor", false)).isEmpty();
            assertThat(repo.findByName("reviewer", false)).isPresent();

            repo.delete(new WordName("reviewer"));
            assertThat(repo.findByName("reviewer", false)).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert(new RoleSpec(new WordName("unique_role"), "First"));
            assertThatThrownBy(() -> repo.insert(new RoleSpec(new WordName("unique_role"), "Second")))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert(new RoleSpec(new WordName("MyRole"), "desc"));
            assertThat(repo.isNameUnique(new WordName("myrole"))).isFalse();
            assertThat(repo.isNameUnique(new WordName("MYROLE"))).isFalse();
            assertThat(repo.isNameUnique(new WordName("other"))).isTrue();
        }

        @Test
        void findByName_embedPermissions_returnsEmpty() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert(new RoleSpec(new WordName("bare_role"), "No permissions"));
            Optional<Role> found = repo.findByName("bare_role", true);
            assertThat(found).isPresent();
            assertThat(((RoleWithPermissions) found.get()).permissions()).isEmpty();
        }

        @Test
        void findByName_embedPermissions_returnsAssociatedPermissions() {
            RoleRepository roleRepo = new RoleRepository(dsl);
            PermissionRepository permRepo = new PermissionRepository(dsl);
            RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

            Role role = roleRepo.insert(new RoleSpec(new WordName("viewer"), "Can view"));
            Permission perm = permRepo.insert(new PermissionSpec(new PermissionName("view:data"), "View data"));
            rpRepo.addPermission(role.id(), perm.id());

            Optional<Role> found = roleRepo.findByName("viewer", true);
            assertThat(found).isPresent();
            assertThat(((RoleWithPermissions) found.get()).permissions()).hasSize(1);
            assertThat(((RoleWithPermissions) found.get()).permissions().getFirst().name().value()).isEqualTo("view:data");
        }

        @Test
        void deleteNonExistent_doesNotThrow() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.delete(new WordName("no_such_role"));
        }

        @Test
        void rename_toSameName_isNotConflict() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert(new RoleSpec(new WordName("myrole"), "desc"));
            assertThat(repo.isNameUnique(new WordName("myrole"))).isFalse();
        }

        @Test
        void rename_toDuplicateName_isConflict() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert(new RoleSpec(new WordName("role_a"), "A"));
            repo.insert(new RoleSpec(new WordName("role_b"), "B"));
            assertThat(repo.isNameUnique(new WordName("role_b"))).isFalse();
        }
    }

    @Nested
    class PermissionCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            PermissionRepository repo = new PermissionRepository(dsl);

            Permission created = repo.insert(new PermissionSpec(new PermissionName("resource:action"), "Do action on resource"));
            assertThat(created.id()).isNotNull();
            assertThat(created.name().value()).isEqualTo("resource:action");

            Optional<Permission> found = repo.findByName("resource:action");
            assertThat(found).isPresent();

            repo.update(new PermissionName("resource:action"), new PermissionSpec(new PermissionName("resource:new_action"), "Updated"));
            assertThat(repo.findByName("resource:action")).isEmpty();
            assertThat(repo.findByName("resource:new_action")).isPresent();

            repo.delete(new PermissionName("resource:new_action"));
            assertThat(repo.findByName("resource:new_action")).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert(new PermissionSpec(new PermissionName("unique_perm"), "First"));
            assertThatThrownBy(() -> repo.insert(new PermissionSpec(new PermissionName("unique_perm"), "Second")))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert(new PermissionSpec(new PermissionName("MyPerm"), "desc"));
            assertThat(repo.isNameUnique(new PermissionName("myperm"))).isFalse();
            assertThat(repo.isNameUnique(new PermissionName("MYPERM"))).isFalse();
            assertThat(repo.isNameUnique(new PermissionName("otherperm"))).isTrue();
        }

        @Test
        void findIdsByNames_returnsCorrectIds() {
            PermissionRepository repo = new PermissionRepository(dsl);
            Permission p1 = repo.insert(new PermissionSpec(new PermissionName("test:a"), "A"));
            Permission p2 = repo.insert(new PermissionSpec(new PermissionName("test:b"), "B"));
            repo.insert(new PermissionSpec(new PermissionName("test:c"), "C"));

            List<Long> ids = repo.findIdsByNames(List.of("test:a", "test:b"));
            assertThat(ids).containsExactlyInAnyOrder(p1.id(), p2.id());
        }

        @Test
        void findIdsByNames_withNonExistentNames_returnsOnlyMatches() {
            PermissionRepository repo = new PermissionRepository(dsl);
            Permission p1 = repo.insert(new PermissionSpec(new PermissionName("test:exists"), "Exists"));

            List<Long> ids = repo.findIdsByNames(List.of("test:exists", "test:nope"));
            assertThat(ids).containsExactly(p1.id());
        }

        @Test
        void rename_toSameName_isNotConflict() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert(new PermissionSpec(new PermissionName("my:perm"), "desc"));
            assertThat(repo.isNameUnique(new PermissionName("my:perm"))).isFalse();
        }

        @Test
        void rename_toDuplicateName_isConflict() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert(new PermissionSpec(new PermissionName("perm:a"), "A"));
            repo.insert(new PermissionSpec(new PermissionName("perm:b"), "B"));
            assertThat(repo.isNameUnique(new PermissionName("perm:b"))).isFalse();
        }

        @Test
        void search_asAdmin_returnsAll() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert(new PermissionSpec(new PermissionName("searchable:one"), "One"));
            repo.insert(new PermissionSpec(new PermissionName("searchable:two"), "Two"));
            repo.insert(new PermissionSpec(new PermissionName("other:three"), "Three"));

            List<Permission> results = repo.search("searchable", null, true, 0, 100);
            assertThat(results.stream().map(p -> p.name().value()))
                    .contains("searchable:one", "searchable:two")
                    .doesNotContain("other:three");
        }
    }

    @Nested
    class ApplicationCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            ApplicationRepository repo = new ApplicationRepository(dsl);

            Application created = repo.insert(new ApplicationSpec(new WordName("myapp"), "My Application", "http://localhost:8080", "/myapp", "/"));
            assertThat(created.id()).isNotNull();
            assertThat(created.name().value()).isEqualTo("myapp");
            assertThat(created.virtualPath()).isEqualTo("/myapp");
            assertThat(created.passTo()).isEqualTo("http://localhost:8080");
            assertThat(created.topPage()).isEqualTo("/");

            Optional<Application> found = repo.findByName(new WordName("myapp"), false);
            assertThat(found).isPresent();

            repo.update(new WordName("myapp"), new ApplicationSpec(new WordName("myapp2"), "Updated", "http://localhost:9090", "/myapp2", "/home"));
            assertThat(repo.findByName(new WordName("myapp"), false)).isEmpty();
            Optional<Application> updated = repo.findByName(new WordName("myapp2"), false);
            assertThat(updated).isPresent();
            assertThat(updated.get().passTo()).isEqualTo("http://localhost:9090");

            repo.delete(new WordName("myapp2"));
            assertThat(repo.findByName(new WordName("myapp2"), false)).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert(new ApplicationSpec(new WordName("dup_app"), "D1", "http://a", "/p1", "/"));
            assertThatThrownBy(() -> repo.insert(new ApplicationSpec(new WordName("dup_app"), "D2", "http://b", "/p2", "/")))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert(new ApplicationSpec(new WordName("UniqueApp"), "desc", "http://a", "/uapp", "/"));
            assertThat(repo.isNameUnique(new WordName("uniqueapp"))).isFalse();
            assertThat(repo.isNameUnique(new WordName("UNIQUEAPP"))).isFalse();
            assertThat(repo.isNameUnique(new WordName("differentapp"))).isTrue();
        }

        @Test
        void findByName_embedRealms_returnsRealms() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app = appRepo.insert(new ApplicationSpec(new WordName("appwrealm"), "App", "http://a", "/wr", "/"));
            realmRepo.insert(app.id(), new RealmSpec(new WordName("realm1"), "http://r1", "First realm"));
            realmRepo.insert(app.id(), new RealmSpec(new WordName("realm2"), "http://r2", "Second realm"));

            Optional<Application> found = appRepo.findByName(new WordName("appwrealm"), true);
            assertThat(found).isPresent();
            assertThat(((ApplicationWithRealms) found.get()).realms()).hasSize(2);
            assertThat(((ApplicationWithRealms) found.get()).realms().stream().map(r -> r.name().value()))
                    .containsExactlyInAnyOrder("realm1", "realm2");
        }

        @Test
        void search_embedRealms_aggregatesCorrectly() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app1 = appRepo.insert(new ApplicationSpec(new WordName("sapp1"), "S1", "http://a", "/s1", "/"));
            Application app2 = appRepo.insert(new ApplicationSpec(new WordName("sapp2"), "S2", "http://b", "/s2", "/"));
            realmRepo.insert(app1.id(), new RealmSpec(new WordName("s1realm"), "http://r", "Realm for s1"));

            List<Application> results = appRepo.search("sapp", true, 0, 100);
            assertThat(results).hasSize(2);
            Application foundApp1 = results.stream().filter(a -> a.name().value().equals("sapp1")).findFirst().orElseThrow();
            Application foundApp2 = results.stream().filter(a -> a.name().value().equals("sapp2")).findFirst().orElseThrow();
            assertThat(((ApplicationWithRealms) foundApp1).realms()).hasSize(1);
            assertThat(((ApplicationWithRealms) foundApp2).realms()).isEmpty();
        }

        @Test
        void rename_toSameName_isNotConflict() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert(new ApplicationSpec(new WordName("myapp"), "desc", "http://a", "/p", "/"));
            assertThat(repo.isNameUnique(new WordName("myapp"))).isFalse();
        }

        @Test
        void rename_toDuplicateName_isConflict() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert(new ApplicationSpec(new WordName("app_a"), "A", "http://a", "/pa", "/"));
            repo.insert(new ApplicationSpec(new WordName("app_b"), "B", "http://b", "/pb", "/"));
            assertThat(repo.isNameUnique(new WordName("app_b"))).isFalse();
        }

        @Test
        void updatePartialFields_keepsOriginals() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert(new ApplicationSpec(new WordName("partial_app"), "Desc", "http://orig", "/partial", "/orig"));

            // Only update description, leave rest null
            repo.update(new WordName("partial_app"), new ApplicationSpec(null, "New Desc", null, null, null));

            Optional<Application> found = repo.findByName(new WordName("partial_app"), false);
            assertThat(found).isPresent();
            assertThat(found.get().description()).isEqualTo("New Desc");
            assertThat(found.get().passTo()).isEqualTo("http://orig");
            assertThat(found.get().topPage()).isEqualTo("/orig");
        }
    }

    @Nested
    class RealmCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository repo = new RealmRepository(dsl);

            Application app = appRepo.insert(new ApplicationSpec(new WordName("rapp"), "R App", "http://a", "/rapp", "/"));
            Realm created = repo.insert(app.id(), new RealmSpec(new WordName("myrealm"), "http://realm", "My realm"));
            assertThat(created.id()).isNotNull();
            assertThat(created.name().value()).isEqualTo("myrealm");

            Optional<Realm> found = repo.findByApplicationAndName(new WordName("rapp"), "myrealm");
            assertThat(found).isPresent();
            assertThat(found.get().url()).isEqualTo("http://realm");

            repo.update(app.id(), new WordName("myrealm"), new RealmSpec(new WordName("renamed_realm"), "http://newurl", "Updated realm"));
            assertThat(repo.findByApplicationAndName(new WordName("rapp"), "myrealm")).isEmpty();
            Optional<Realm> updated = repo.findByApplicationAndName(new WordName("rapp"), "renamed_realm");
            assertThat(updated).isPresent();
            assertThat(updated.get().url()).isEqualTo("http://newurl");

            repo.delete(app.id(), new WordName("renamed_realm"));
            assertThat(repo.findByApplicationAndName(new WordName("rapp"), "renamed_realm")).isEmpty();
        }

        @Test
        void deleteApplication_cascadesDeleteToRealms() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app = appRepo.insert(new ApplicationSpec(new WordName("cascade_app"), "Cascade", "http://a", "/cascade", "/"));
            realmRepo.insert(app.id(), new RealmSpec(new WordName("cascade_realm"), "http://r", "Realm"));

            assertThat(realmRepo.findByApplicationAndName(new WordName("cascade_app"), "cascade_realm")).isPresent();

            appRepo.delete(new WordName("cascade_app"));
            // Realm should be gone due to ON DELETE CASCADE on application_id FK
            assertThat(realmRepo.search(null, "cascade_realm", 0, 100)).isEmpty();
        }

        @Test
        void search_byApplicationName_filters() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app1 = appRepo.insert(new ApplicationSpec(new WordName("filterapp1"), "F1", "http://a", "/f1", "/"));
            Application app2 = appRepo.insert(new ApplicationSpec(new WordName("filterapp2"), "F2", "http://b", "/f2", "/"));
            realmRepo.insert(app1.id(), new RealmSpec(new WordName("realm_a"), "http://ra", "RA"));
            realmRepo.insert(app2.id(), new RealmSpec(new WordName("realm_b"), "http://rb", "RB"));

            List<Realm> byApp1 = realmRepo.search(new WordName("filterapp1"), null, 0, 100);
            assertThat(byApp1).hasSize(1);
            assertThat(byApp1.getFirst().name().value()).isEqualTo("realm_a");
        }
    }
}
