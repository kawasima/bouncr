package net.unit8.bouncr.api.resource.me;

import enkan.component.jpa.EntityManagerProvider;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;
import net.unit8.bouncr.api.resource.PasswordCredentialResource;
import net.unit8.bouncr.api.resource.UsersResourceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class PasswordCredentialResourceTest {
    private EntityManager em;
    private EnkanSystem system;
    private static final Logger LOG = LoggerFactory.getLogger(UsersResourceTest.class);

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
    public void create() {
        PasswordCredentialResource resource = new PasswordCredentialResource();
    }
}
