package net.unit8.bouncr.api.resource;

import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.jpa.EntityManagerProvider;
import enkan.data.DefaultHttpRequest;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.BouncrTestSystemFactory;
import net.unit8.bouncr.api.boundary.PasswordSignInRequest;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.component.BouncrConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static org.mockito.MockitoAnnotations.initMocks;

public class PasswordSignInResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordSignInResourceTest.class);

    private EnkanSystem system;

    @BeforeEach
    void setup() {
        system = EnkanSystem.of(
                "converter", new JacksonBeansConverter(),
                "validator", new BeansValidator(),
                "config",    new BouncrConfiguration()
        );
        system.start();
    }

    @Test
    void authenticationSuccessful() {
        ComponentInjector injector = new ComponentInjector(
                Map.of("converter", system.getComponent("converter"),
                        "validator", system.getComponent("validator"),
                        "config", system.getComponent("config")));

        final PasswordSignInResource resource = injector.inject(new PasswordSignInResource());
        PasswordSignInRequest request = builder(new PasswordSignInRequest())
                .set(PasswordSignInRequest::setAccount, "kawasima")
                .set(PasswordSignInRequest::setPassword, "pass1234")
                .build();
        ActionRecord record = builder(new ActionRecord())
                .build();
        final RestContext context = new RestContext(new DefaultResource(), new DefaultHttpRequest());
        final EntityManager em = MockFactory.createEntityManagerMock();

        resource.authenticate(request, record, context, em);
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }
}
