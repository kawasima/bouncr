package net.unit8.bouncr.api.resource;

import enkan.component.eclipselink.EclipseLinkEntityManagerProvider;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.system.EnkanSystem;
import enkan.util.jpa.EntityTransactionManager;
import net.unit8.bouncr.BouncrTestSystemFactory;
import net.unit8.bouncr.api.boundary.UserSearchParams;
import net.unit8.bouncr.entity.Application;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Realm;
import net.unit8.bouncr.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;

class UsersResourceWithDbTest {
    private EnkanSystem system;

    @BeforeEach
    void setup() {
        system = new BouncrTestSystemFactory().create();
        system.start();
    }

    @Test
    void test() {
        final UsersResource resource = new UsersResource();
        final EclipseLinkEntityManagerProvider jpa = system.getComponent("jpa", EclipseLinkEntityManagerProvider.class);
        EntityManager em = jpa.getEntityManagerFactory().createEntityManager();

        final UserSearchParams userSearchParams = builder(new UserSearchParams()).build();
        final UserPermissionPrincipal principal = new UserPermissionPrincipal(2L, "test1", Map.of(), Set.of());

        try {

            resource.handleOk(userSearchParams, principal, em);
        } finally {
            em.close();
        }
    }

    private void insertTestUser(EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);

        Application testApp = builder(new Application())
                .set(Application::setName, "test")
                .set(Application::setDescription, "test")
                .set(Application::setVirtualPath, "/test")
                .set(Application::setTopPage, "/test")
                .set(Application::setPassTo, "/test")
                .set(Application::setNameLower, "test")
                .set(Application::setWriteProtected, false)
                .build();
        Realm realm = builder(new Realm())
                .set(Realm::setName, "testRealm")
                .set(Realm::setNameLower, "testrealm")
                .set(Realm::setDescription, "testRealm")
                .set(Realm::setUrl, ".*")
                .set(Realm::setApplication, testApp)
                .set(Realm::setWriteProtected, false)
                .build();
        testApp.setRealms(List.of(realm));

        final Role role = builder(new Role())
                .set(Role::setName, "test")
                .set(Role::setNameLower, "test")
                .set(Role::setDescription, "test")
                .set(Role::setWriteProtected, false)
                .build();

        final Group group = builder(new Group())
                .set(Group::setName, "test")
                .set(Group::setNameLower, "test")
                .set(Group::setDescription, "test")
                .set(Group::setWriteProtected, false)
                .build();
    }
    @AfterEach
    void stopSystem() {
        system.stop();
    }
}
