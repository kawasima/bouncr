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

            Group created = repo.insert("developers", "Dev team");
            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("developers");
            assertThat(created.description()).isEqualTo("Dev team");
            assertThat(created.writeProtected()).isFalse();

            Optional<Group> found = repo.findByName("developers", false);
            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(created.id());

            repo.update("developers", "devs", "Development team");
            assertThat(repo.findByName("developers", false)).isEmpty();
            Optional<Group> updated = repo.findByName("devs", false);
            assertThat(updated).isPresent();
            assertThat(updated.get().description()).isEqualTo("Development team");

            repo.delete("devs");
            assertThat(repo.findByName("devs", false)).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert("unique_group", "First");
            assertThatThrownBy(() -> repo.insert("unique_group", "Second"))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert("TestGroup", "desc");
            assertThat(repo.isNameUnique("testgroup")).isFalse();
            assertThat(repo.isNameUnique("TESTGROUP")).isFalse();
            assertThat(repo.isNameUnique("other_group")).isTrue();
        }

        @Test
        void deleteNonExistent_doesNotThrow() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.delete("no_such_group"); // should not throw
        }

        @Test
        void updateToNull_keepsOriginalValues() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert("mygroup", "original desc");

            // Update with null newName and null description - should keep originals
            repo.update("mygroup", null, null);
            Optional<Group> found = repo.findByName("mygroup", false);
            assertThat(found).isPresent();
            assertThat(found.get().description()).isEqualTo("original desc");
        }

        @Test
        void findByName_embedUsers_returnsMembers() {
            GroupRepository groupRepo = new GroupRepository(dsl);
            UserRepository userRepo = new UserRepository(dsl);

            Group group = groupRepo.insert("team", "A team");
            User user1 = userRepo.insert("alice");
            User user2 = userRepo.insert("bob");
            groupRepo.addUser("team", user1.id());
            groupRepo.addUser("team", user2.id());

            Optional<Group> found = groupRepo.findByName("team", true);
            assertThat(found).isPresent();
            assertThat(found.get().users()).hasSize(2);
            assertThat(found.get().users().stream().map(User::account))
                    .containsExactlyInAnyOrder("alice", "bob");
        }

        @Test
        void findByName_noEmbedUsers_returnsNullUsers() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert("team", "A team");
            Optional<Group> found = repo.findByName("team", false);
            assertThat(found).isPresent();
            assertThat(found.get().users()).isNull();
        }

        @Test
        void search_withQuery_filtersResults() {
            GroupRepository repo = new GroupRepository(dsl);
            repo.insert("alpha_team", "Alpha");
            repo.insert("beta_team", "Beta");
            repo.insert("gamma_squad", "Gamma");

            List<Group> results = repo.search("team", null, true, 0, 100);
            assertThat(results.stream().map(Group::name))
                    .contains("alpha_team", "beta_team")
                    .doesNotContain("gamma_squad");
        }

        @Test
        void addUser_toNonExistentGroup_throwsIllegalArgument() {
            GroupRepository repo = new GroupRepository(dsl);
            UserRepository userRepo = new UserRepository(dsl);
            User user = userRepo.insert("orphan");

            assertThatThrownBy(() -> repo.addUser("nonexistent_group", user.id()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Group not found");
        }
    }

    @Nested
    class RoleCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            RoleRepository repo = new RoleRepository(dsl);

            Role created = repo.insert("editor", "Can edit");
            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("editor");

            Optional<Role> found = repo.findByName("editor", false);
            assertThat(found).isPresent();

            repo.update("editor", "reviewer", "Can review");
            assertThat(repo.findByName("editor", false)).isEmpty();
            assertThat(repo.findByName("reviewer", false)).isPresent();

            repo.delete("reviewer");
            assertThat(repo.findByName("reviewer", false)).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert("unique_role", "First");
            assertThatThrownBy(() -> repo.insert("unique_role", "Second"))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert("MyRole", "desc");
            assertThat(repo.isNameUnique("myrole")).isFalse();
            assertThat(repo.isNameUnique("MYROLE")).isFalse();
            assertThat(repo.isNameUnique("other")).isTrue();
        }

        @Test
        void findByName_embedPermissions_returnsEmpty() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.insert("bare_role", "No permissions");
            Optional<Role> found = repo.findByName("bare_role", true);
            assertThat(found).isPresent();
            assertThat(found.get().permissions()).isEmpty();
        }

        @Test
        void findByName_embedPermissions_returnsAssociatedPermissions() {
            RoleRepository roleRepo = new RoleRepository(dsl);
            PermissionRepository permRepo = new PermissionRepository(dsl);
            RolePermissionRepository rpRepo = new RolePermissionRepository(dsl);

            Role role = roleRepo.insert("viewer", "Can view");
            Permission perm = permRepo.insert("view:data", "View data");
            rpRepo.addPermission(role.id(), perm.id());

            Optional<Role> found = roleRepo.findByName("viewer", true);
            assertThat(found).isPresent();
            assertThat(found.get().permissions()).hasSize(1);
            assertThat(found.get().permissions().getFirst().name()).isEqualTo("view:data");
        }

        @Test
        void deleteNonExistent_doesNotThrow() {
            RoleRepository repo = new RoleRepository(dsl);
            repo.delete("no_such_role");
        }
    }

    @Nested
    class PermissionCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            PermissionRepository repo = new PermissionRepository(dsl);

            Permission created = repo.insert("resource:action", "Do action on resource");
            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("resource:action");

            Optional<Permission> found = repo.findByName("resource:action");
            assertThat(found).isPresent();

            repo.update("resource:action", "resource:new_action", "Updated");
            assertThat(repo.findByName("resource:action")).isEmpty();
            assertThat(repo.findByName("resource:new_action")).isPresent();

            repo.delete("resource:new_action");
            assertThat(repo.findByName("resource:new_action")).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert("unique_perm", "First");
            assertThatThrownBy(() -> repo.insert("unique_perm", "Second"))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert("MyPerm", "desc");
            assertThat(repo.isNameUnique("myperm")).isFalse();
            assertThat(repo.isNameUnique("MYPERM")).isFalse();
            assertThat(repo.isNameUnique("otherperm")).isTrue();
        }

        @Test
        void findIdsByNames_returnsCorrectIds() {
            PermissionRepository repo = new PermissionRepository(dsl);
            Permission p1 = repo.insert("test:a", "A");
            Permission p2 = repo.insert("test:b", "B");
            repo.insert("test:c", "C");

            List<Long> ids = repo.findIdsByNames(List.of("test:a", "test:b"));
            assertThat(ids).containsExactlyInAnyOrder(p1.id(), p2.id());
        }

        @Test
        void findIdsByNames_withNonExistentNames_returnsOnlyMatches() {
            PermissionRepository repo = new PermissionRepository(dsl);
            Permission p1 = repo.insert("test:exists", "Exists");

            List<Long> ids = repo.findIdsByNames(List.of("test:exists", "test:nope"));
            assertThat(ids).containsExactly(p1.id());
        }

        @Test
        void search_asAdmin_returnsAll() {
            PermissionRepository repo = new PermissionRepository(dsl);
            repo.insert("searchable:one", "One");
            repo.insert("searchable:two", "Two");
            repo.insert("other:three", "Three");

            List<Permission> results = repo.search("searchable", null, true, 0, 100);
            assertThat(results.stream().map(Permission::name))
                    .contains("searchable:one", "searchable:two")
                    .doesNotContain("other:three");
        }
    }

    @Nested
    class ApplicationCrud {
        @Test
        void fullLifecycle_insertFindUpdateDelete() {
            ApplicationRepository repo = new ApplicationRepository(dsl);

            Application created = repo.insert("myapp", "My Application", "/myapp", "http://localhost:8080", "/");
            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("myapp");
            assertThat(created.virtualPath()).isEqualTo("/myapp");
            assertThat(created.passTo()).isEqualTo("http://localhost:8080");
            assertThat(created.topPage()).isEqualTo("/");

            Optional<Application> found = repo.findByName("myapp", false);
            assertThat(found).isPresent();

            repo.update("myapp", "myapp2", "Updated", "/myapp2", "http://localhost:9090", "/home");
            assertThat(repo.findByName("myapp", false)).isEmpty();
            Optional<Application> updated = repo.findByName("myapp2", false);
            assertThat(updated).isPresent();
            assertThat(updated.get().passTo()).isEqualTo("http://localhost:9090");

            repo.delete("myapp2");
            assertThat(repo.findByName("myapp2", false)).isEmpty();
        }

        @Test
        void insertDuplicateName_throwsConstraintViolation() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert("dup_app", "D1", "/p1", "http://a", "/");
            assertThatThrownBy(() -> repo.insert("dup_app", "D2", "/p2", "http://b", "/"))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        void isNameUnique_caseInsensitive() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert("UniqueApp", "desc", "/uapp", "http://a", "/");
            assertThat(repo.isNameUnique("uniqueapp")).isFalse();
            assertThat(repo.isNameUnique("UNIQUEAPP")).isFalse();
            assertThat(repo.isNameUnique("differentapp")).isTrue();
        }

        @Test
        void findByName_embedRealms_returnsRealms() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app = appRepo.insert("appwrealm", "App", "/wr", "http://a", "/");
            realmRepo.insert(app.id(), "realm1", "http://r1", "First realm");
            realmRepo.insert(app.id(), "realm2", "http://r2", "Second realm");

            Optional<Application> found = appRepo.findByName("appwrealm", true);
            assertThat(found).isPresent();
            assertThat(found.get().realms()).hasSize(2);
            assertThat(found.get().realms().stream().map(Realm::name))
                    .containsExactlyInAnyOrder("realm1", "realm2");
        }

        @Test
        void search_embedRealms_aggregatesCorrectly() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app1 = appRepo.insert("sapp1", "S1", "/s1", "http://a", "/");
            Application app2 = appRepo.insert("sapp2", "S2", "/s2", "http://b", "/");
            realmRepo.insert(app1.id(), "s1realm", "http://r", "Realm for s1");

            List<Application> results = appRepo.search("sapp", true, 0, 100);
            assertThat(results).hasSize(2);
            Application foundApp1 = results.stream().filter(a -> a.name().equals("sapp1")).findFirst().orElseThrow();
            Application foundApp2 = results.stream().filter(a -> a.name().equals("sapp2")).findFirst().orElseThrow();
            assertThat(foundApp1.realms()).hasSize(1);
            assertThat(foundApp2.realms()).isEmpty();
        }

        @Test
        void updatePartialFields_keepsOriginals() {
            ApplicationRepository repo = new ApplicationRepository(dsl);
            repo.insert("partial_app", "Desc", "/partial", "http://orig", "/orig");

            // Only update description, leave rest null
            repo.update("partial_app", null, "New Desc", null, null, null);

            Optional<Application> found = repo.findByName("partial_app", false);
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

            Application app = appRepo.insert("rapp", "R App", "/rapp", "http://a", "/");
            Realm created = repo.insert(app.id(), "myrealm", "http://realm", "My realm");
            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("myrealm");

            Optional<Realm> found = repo.findByApplicationAndName("rapp", "myrealm");
            assertThat(found).isPresent();
            assertThat(found.get().url()).isEqualTo("http://realm");

            repo.update(app.id(), "myrealm", "renamed_realm", "http://newurl", "Updated realm");
            assertThat(repo.findByApplicationAndName("rapp", "myrealm")).isEmpty();
            Optional<Realm> updated = repo.findByApplicationAndName("rapp", "renamed_realm");
            assertThat(updated).isPresent();
            assertThat(updated.get().url()).isEqualTo("http://newurl");

            repo.delete(app.id(), "renamed_realm");
            assertThat(repo.findByApplicationAndName("rapp", "renamed_realm")).isEmpty();
        }

        @Test
        void deleteApplication_cascadesDeleteToRealms() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app = appRepo.insert("cascade_app", "Cascade", "/cascade", "http://a", "/");
            realmRepo.insert(app.id(), "cascade_realm", "http://r", "Realm");

            assertThat(realmRepo.findByApplicationAndName("cascade_app", "cascade_realm")).isPresent();

            appRepo.delete("cascade_app");
            // Realm should be gone due to ON DELETE CASCADE on application_id FK
            assertThat(realmRepo.search(null, "cascade_realm", 0, 100)).isEmpty();
        }

        @Test
        void search_byApplicationName_filters() {
            ApplicationRepository appRepo = new ApplicationRepository(dsl);
            RealmRepository realmRepo = new RealmRepository(dsl);

            Application app1 = appRepo.insert("filterapp1", "F1", "/f1", "http://a", "/");
            Application app2 = appRepo.insert("filterapp2", "F2", "/f2", "http://b", "/");
            realmRepo.insert(app1.id(), "realm_a", "http://ra", "RA");
            realmRepo.insert(app2.id(), "realm_b", "http://rb", "RB");

            List<Realm> byApp1 = realmRepo.search("filterapp1", null, 0, 100);
            assertThat(byApp1).hasSize(1);
            assertThat(byApp1.getFirst().name()).isEqualTo("realm_a");
        }
    }
}
