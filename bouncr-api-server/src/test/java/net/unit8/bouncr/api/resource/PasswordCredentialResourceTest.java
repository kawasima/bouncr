package net.unit8.bouncr.api.resource;

import enkan.component.jackson.JacksonBeansConverter;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.PasswordCredentialCreateRequest;
import net.unit8.bouncr.api.boundary.PasswordCredentialUpdateRequest;
import net.unit8.bouncr.api.resource.PasswordCredentialResource;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.PasswordCredential;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.PasswordUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.Map;
import java.util.stream.Stream;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PasswordCredentialResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordCredentialResourceTest.class);

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
    void validateCreateRequest_Error() {
        TypedQuery query = mock(TypedQuery.class);
        final EntityManager em = MockFactory.createEntityManagerMock(query);
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

    @Test
    void validateCreateRequest_Success() {
        TypedQuery query = mock(TypedQuery.class);
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();

        RestContext context = new RestContext(new DefaultResource(), request);
        PasswordCredentialResource resource = new PasswordCredentialResource();
        PasswordCredentialCreateRequest createRequest = new PasswordCredentialCreateRequest();
        createRequest.setAccount("test_user");
        createRequest.setPassword("pass1234");

        ComponentInjector injector = new ComponentInjector(
                Map.of("config",   system.getComponent("config"),
                        "validator", system.getComponent("validator")));

        injector.inject(resource);
        Problem problem = resource.validateCreateRequest(createRequest, context, em);
        assertThat(problem).isNull();
    }

    @Test
    void userProcessableInPost_Successful() {
        TypedQuery query = mock(TypedQuery.class);
        User user = builder(new User())
                .set(User::setId, 1L)
                .set(User::setAccount, "test_user")
                .build();
        when(query.getResultStream()).thenReturn(Stream.of(user));
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();

        RestContext context = new RestContext(new DefaultResource(), request);
        PasswordCredentialResource resource = new PasswordCredentialResource();
        PasswordCredentialCreateRequest createRequest = new PasswordCredentialCreateRequest();
        createRequest.setAccount("test_user");
        createRequest.setPassword("pass1234");

        ComponentInjector injector = new ComponentInjector(
                Map.of("config",   system.getComponent("config"),
                        "validator", system.getComponent("validator")));

        injector.inject(resource);
        boolean ret = resource.userProcessableInPost(createRequest, context, em);
        assertThat(ret).isTrue();
    }

    @Test
    void userProcessableInPut_Successful() {
        TypedQuery query = mock(TypedQuery.class);
        PasswordCredential credential = builder(new PasswordCredential())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2("pass1234", "saltsalt", 100))
                .set(PasswordCredential::setSalt, "saltsalt")
                .build();
        User user = builder(new User())
                .set(User::setId, 1L)
                .set(User::setAccount, "test_user")
                .set(User::setPasswordCredential, credential)
                .build();
        when(query.getResultStream()).thenReturn(Stream.of(user));
        final EntityManager em = MockFactory.createEntityManagerMock(query);
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();

        RestContext context = new RestContext(new DefaultResource(), request);
        PasswordCredentialResource resource = new PasswordCredentialResource();
        PasswordCredentialUpdateRequest updateRequest = builder(new PasswordCredentialUpdateRequest())
                .set(PasswordCredentialUpdateRequest::setAccount, "test_user")
                .set(PasswordCredentialUpdateRequest::setOldPassword, "pass1234")
                .set(PasswordCredentialUpdateRequest::setNewPassword, "pass5678")
                .build();

        ComponentInjector injector = new ComponentInjector(
                Map.of("config",   system.getComponent("config"),
                        "validator", system.getComponent("validator")));

        injector.inject(resource);
        boolean ret = resource.userProcessableInPut(updateRequest, context, em);
        assertThat(ret).isTrue();
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }
}
