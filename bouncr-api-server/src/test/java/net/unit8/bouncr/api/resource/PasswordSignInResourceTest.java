package net.unit8.bouncr.api.resource;

import enkan.component.jpa.EntityManagerProvider;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class PasswordSignInResourceTest {
    private EntityManager em;
    private EnkanSystem system;
    private static final Logger LOG = LoggerFactory.getLogger(PasswordSignInResourceTest.class);

    @BeforeEach
    void setup() {
        system = new BouncrApiEnkanSystemFactory().create();
        system.start();
        EntityManagerProvider provider = system.getComponent("jpa");
        em = provider.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }
}
