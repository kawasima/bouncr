package net.unit8.bouncr.api.resource;

import enkan.component.jpa.EntityManagerProvider;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.BouncrTestSystemFactory;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;
import net.unit8.bouncr.api.boundary.PasswordCredentialCreateRequest;
import net.unit8.bouncr.api.resource.PasswordCredentialResource;
import net.unit8.bouncr.api.resource.UsersResourceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class PasswordCredentialResourceTest {
    private EntityManager em;
    private EnkanSystem system;
    private static final Logger LOG = LoggerFactory.getLogger(UsersResourceTest.class);

    @BeforeEach
    void setup() {
        system = new BouncrTestSystemFactory().create();
        system.start();
        EntityManagerProvider provider = system.getComponent("jpa");
        em = provider.createEntityManager();
    }

    @Test
    void validateCreateRequest_Error() {
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();

        RestContext context = new RestContext(new DefaultResource(), request);
        PasswordCredentialResource resource = new PasswordCredentialResource();
        PasswordCredentialCreateRequest createRequest = new PasswordCredentialCreateRequest();

        ComponentInjector injector = new ComponentInjector(
                Map.of("config",   system.getComponent("config"),
                        "validator", system.getComponent("validator")));

        injector.inject(resource);
        Problem problem = resource.validateCreateRequest(createRequest, context, em);

        assertThat(problem).isNotNull();
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }
}
