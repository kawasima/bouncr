package net.unit8.bouncr.api.resource;

import enkan.component.jackson.JacksonBeansConverter;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.UserCreateRequest;
import net.unit8.bouncr.api.boundary.UserSearchParams;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class UsersResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(UsersResourceTest.class);

    private EnkanSystem system;

    @BeforeEach
    void setup() {
        initMocks(this);
        system = EnkanSystem.of(
                "converter", new JacksonBeansConverter(),
                "validator", new BeansValidator(),
                "config",    new BouncrConfiguration()
        );
        system.start();
    }

    EntityManager createEntityManagerMock(Object... mocks) {
        List<Object> mockList = Arrays.asList(mocks);
        EntityManager em = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        CriteriaQuery query = mockList.stream()
                .filter(CriteriaQuery.class::isInstance)
                .map(CriteriaQuery.class::cast)
                .findAny()
                .orElse(mock(CriteriaQuery.class));
        Root<?> root = mockList.stream()
                .filter(Root.class::isInstance)
                .map(Root.class::cast)
                .findAny()
                .orElse(mock(Root.class));
        Join join = mock(Join.class);
        EntityGraph graph = mockList.stream()
                .filter(EntityGraph.class::isInstance)
                .map(EntityGraph.class::cast)
                .findAny()
                .orElse(mock(EntityGraph.class));
        Subgraph subgraph = mock(Subgraph.class);

        TypedQuery typedQuery = mockList.stream()
                .filter(TypedQuery.class::isInstance)
                .map(TypedQuery.class::cast)
                .findAny()
                .orElse(mock(TypedQuery.class));

        EntityTransaction tx = mockList.stream()
                .filter(EntityTransaction.class::isInstance)
                .map(EntityTransaction.class::cast)
                .findAny()
                .orElse(mock(EntityTransaction.class));

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(any(Class.class))).thenReturn(query);
        when(query.from(any(Class.class))).thenReturn(root);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(em.createEntityGraph(any(Class.class))).thenReturn(graph);
        when(root.join(anyString())).thenReturn(join);
        when(join.join(anyString())).thenReturn(join);
        when(graph.addSubgraph(anyString())).thenReturn(subgraph);
        when(subgraph.addSubgraph(anyString())).thenReturn(subgraph);
        when(em.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(typedQuery.setHint(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(em.getTransaction()).thenReturn(tx);
        return em;
    }

    @Test
    void findDefaultUsers() {
        CriteriaQuery query = mock(CriteriaQuery.class);
        TypedQuery typedQuery = mock(TypedQuery.class);
        EntityGraph graph = mock(EntityGraph.class);

        EntityManager em = createEntityManagerMock(typedQuery, graph, query);
        UsersResource resource = new UsersResource();
        UserSearchParams params = builder(new UserSearchParams())
                .set(UserSearchParams::setGroupId, 1L)
                .set(UserSearchParams::setEmbed, "(groups)")
                .build();
        UserPermissionPrincipal principal = new UserPermissionPrincipal(1L, "admin", Map.of(), Set.of("user:read"));
        resource.handleOk(params, principal, em);
        verify(query, times(0)).where();
        verify(typedQuery).setFirstResult(eq(0));
        verify(typedQuery).setMaxResults(eq(10));
        verify(graph).addSubgraph(eq("groups"));
    }

    @Test
    void findByNoSuchGroupId() {
        CriteriaQuery query = mock(CriteriaQuery.class);
        TypedQuery typedQuery = mock(TypedQuery.class);
        EntityGraph graph = mock(EntityGraph.class);
        Root<User> userRoot = mock(Root.class);

        EntityManager em = createEntityManagerMock(typedQuery, graph, query, userRoot);
        UsersResource resource = new UsersResource();

        UserSearchParams params = builder(new UserSearchParams())
                .set(UserSearchParams::setGroupId, 10L)
                .build();
        UserPermissionPrincipal principal = new UserPermissionPrincipal(1L, "admin", Map.of(), Set.of("any_user:read"));
        resource.handleOk(params, principal, em);
        verify(userRoot).join(eq("groups"));
    }

    @Test
    void post() {
        UsersResource resource = new UsersResource();
        ComponentInjector injector = new ComponentInjector(
                Map.of("converter", system.getComponent("converter"),
                        "config", system.getComponent("config")));
        injector.inject(resource);
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();
        RestContext context = new RestContext(new DefaultResource(), request);
        UserCreateRequest user = builder(new UserCreateRequest())
                .set(UserCreateRequest::setAccount, "fuga")
                .build();
        EntityManager em = createEntityManagerMock();
        resource.doPost(user, context, em);
    }

    @Test
    void validate() {
        ComponentInjector injector = new ComponentInjector(
                Map.of("converter", system.getComponent("converter"),
                        "validator", system.getComponent("validator"),
                        "config", system.getComponent("config")));
        UsersResource resource = injector.inject(new UsersResource());
        UserCreateRequest createRequest = builder(new UserCreateRequest())
                .set(UserCreateRequest::setAccount, "fuga")
                .build();
        HttpRequest request = builder(new DefaultHttpRequest())
                .set(HttpRequest::setRequestMethod, "POST")
                .build();
        EntityManager em = createEntityManagerMock();
        Problem problem = resource.validateUserCreateRequest(createRequest, new RestContext(new DefaultResource(), request), em);
        assertThat(problem).isNull();
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }
}
