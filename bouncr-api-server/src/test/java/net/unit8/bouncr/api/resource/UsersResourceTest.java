package net.unit8.bouncr.api.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.component.jpa.EntityManagerProvider;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;
import net.unit8.bouncr.api.boundary.UserCreateRequest;
import net.unit8.bouncr.api.boundary.UserSearchParams;
import net.unit8.bouncr.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class UsersResourceTest {
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
    public void findDefaultUsers() throws Exception {
        UsersResource resource = new UsersResource();
        UserSearchParams params = builder(new UserSearchParams())
                .set(UserSearchParams::setGroupId, 1L)
                .set(UserSearchParams::setEmbed, "(groups)")
                .build();
        List<User> users = resource.handleOk(params, em);
        ObjectMapper mapper = new ObjectMapper();
        LOG.debug("{}\n{}", users, mapper.writeValueAsString(users));
        assertThat(users).hasSize(1);
    }

    @Test
    public void findByNoSuchGroupId() {
        UsersResource resource = new UsersResource();
        UserSearchParams params = builder(new UserSearchParams())
                .set(UserSearchParams::setGroupId, 10L)
                .build();
        List<User> users = resource.handleOk(params, em);
        assertThat(users).isEmpty();
    }

    @Test
    public void post() {
        UsersResource resource = new UsersResource();
        UserCreateRequest user = builder(new UserCreateRequest())
                .set(UserCreateRequest::setAccount, "fuga")
                .build();
        resource.doPost(user, em);
    }

    @Test
    public void validate() {
        ComponentInjector injector = new ComponentInjector(
                Map.of("converter", system.getComponent("converter"),
                        "validator", system.getComponent("validator")));
        UsersResource resource = injector.inject(new UsersResource());
        UserCreateRequest createRequest = builder(new UserCreateRequest())
                .set(UserCreateRequest::setAccount, "fuga")
                .build();
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();
        Problem problem = resource.validateUserCreateRequest(createRequest, new RestContext(new DefaultResource(), request), em);
        assertThat(problem).isNull();
        assertThat(resource.validateUserCreateRequest(createRequest, new RestContext(new DefaultResource(), request), em))
                .isNotNull();

    }
}
