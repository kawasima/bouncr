package net.unit8.bouncr.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.component.jpa.EntityManagerProvider;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.data.DefaultResouruce;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.BouncrEnkanSystem;
import net.unit8.bouncr.web.boundary.CreateUserRequest;
import net.unit8.bouncr.web.boundary.UserSearchParam;
import net.unit8.bouncr.web.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
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
        system = new BouncrEnkanSystem().create();
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
        UserSearchParam params = builder(new UserSearchParam())
                .set(UserSearchParam::setGroupId, 1L)
                .set(UserSearchParam::setEmbed, "(groups)")
                .build();
        List<User> users = resource.handleOk(params, em);
        ObjectMapper mapper = new ObjectMapper();
        LOG.debug("{}\n{}", users, mapper.writeValueAsString(users));
        assertThat(users).hasSize(1);
    }

    @Test
    public void findByNoSuchGroupId() {
        UsersResource resource = new UsersResource();
        UserSearchParam params = builder(new UserSearchParam())
                .set(UserSearchParam::setGroupId, 10L)
                .build();
        List<User> users = resource.handleOk(params, em);
        assertThat(users).isEmpty();
    }

    @Test
    public void post() {
        UsersResource resource = new UsersResource();
        User user = builder(new User())
                .set(User::setAccount, "fuga")
                .set(User::setName, "hoge")
                .set(User::setEmail, "hoge@example.com")
                .set(User::setWriteProtected, true)
                .build();
        resource.doPost(user, em);
        assertThat(user.getId()).isNotNull();
    }

    @Test
    public void validate() {
        ComponentInjector injector = new ComponentInjector(
                Map.of("jackson", system.getComponent("jackson"),
                        "validator", system.getComponent("validator")));
        UsersResource resource = injector.inject(new UsersResource());
        CreateUserRequest createRequest = builder(new CreateUserRequest())
                .set(CreateUserRequest::setAccount, "fuga")
                .set(CreateUserRequest::setName, "hoge")
                .set(CreateUserRequest::setEmail, "hoge@example.com")
                .build();
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();
        Problem problem = resource.validateUserCreateRequest(createRequest, new RestContext(new DefaultResouruce(), request));
        assertThat(problem).isNull();
        createRequest.setEmail("xxx");
        assertThat(resource.validateUserCreateRequest(createRequest, new RestContext(new DefaultResouruce(), request)))
                .isNotNull();

    }
}
