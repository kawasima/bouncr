package net.unit8.bouncr.api.resource;

import enkan.component.jpa.EntityManagerProvider;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;
import net.unit8.bouncr.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Map;

public class PasswordSignInResourceTest {
    private EntityManager em;
    private EnkanSystem system;
    private static final Logger LOG = LoggerFactory.getLogger(PasswordSignInResourceTest.class);

    @BeforeEach
    public void setup() {
        system = new BouncrApiEnkanSystemFactory().create();
        system.start();
        EntityManagerProvider provider = system.getComponent("jpa");
        em = provider.createEntityManager();
    }

    @AfterEach
    public void tearDown() {
        system.stop();
    }

    @Test
    public void permissionsByRealm() {
        PasswordSignInResource resource = new PasswordSignInResource();
        User user = em.find(User.class, 1L);
        Map<Long, UserPermissionPrincipal> permissionsByRealm = resource.getPermissionsByRealm(user, em);
        System.out.println(permissionsByRealm);
    }
}
