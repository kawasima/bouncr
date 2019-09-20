package net.unit8.bouncr.api.resource;

import enkan.component.jackson.JacksonBeansConverter;
import enkan.data.DefaultHttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.system.EnkanSystem;
import enkan.system.inject.ComponentInjector;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.DefaultResource;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.UserUpdateRequest;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileField;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.entity.UserProfileVerification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class UserResourceTest {
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

    @Test
    void updateUserWithEmail() {
        CriteriaQuery query = mock(CriteriaQuery.class);
        TypedQuery typedQuery = mock(TypedQuery.class);
        EntityGraph graph = mock(EntityGraph.class);

        EntityManager em = MockFactory.createEntityManagerMock(typedQuery, graph, query);

        UserResource sat = new UserResource();
        ComponentInjector injector = new ComponentInjector(
                Map.of("converter", system.getComponent("converter"),
                        "validator", system.getComponent("validator"),
                        "config", system.getComponent("config")));
        injector.inject(sat);

        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setUserProfile("email", "kawasima1@example.com");

        UserProfileField emailField = builder(new UserProfileField())
                .set(UserProfileField::setId, 1L)
                .set(UserProfileField::setJsonName, "email")
                .set(UserProfileField::setNeedsVerification, true)
                .build();
        User user = builder(new User())
                .set(User::setAccount, "kawasima")
                .set(User::setUserProfileValues, new ArrayList<>(List.of(
                        builder(new UserProfileValue())
                                .set(UserProfileValue::setUserProfileField, emailField)
                                .set(UserProfileValue::setValue, "kawasima0@example.com")
                                .build()
                )))
                .build();

        Mockito.when(typedQuery.getResultList()).thenReturn(
                List.of(emailField),
                List.of()
        );

        ActionRecord actionRecord = new ActionRecord();
        UserPermissionPrincipal principal = new UserPermissionPrincipal(1L, "kawasima", Map.of("email", "kawasima0@example.com"), Set.of());
        RestContext context = new RestContext(new DefaultResource(), new DefaultHttpRequest());
        sat.update(updateRequest, user, actionRecord, principal, context, em);

        verify(em).persist(eq(builder(new UserProfileVerification())
                .set(UserProfileVerification::setUser, user)
                .set(UserProfileVerification::setUserProfileField, emailField)
                .build()));
    }

    @Test
    void updateUserWithoutEmail() {
        CriteriaQuery query = mock(CriteriaQuery.class);
        TypedQuery typedQuery = mock(TypedQuery.class);
        EntityGraph graph = mock(EntityGraph.class);

        EntityManager em = MockFactory.createEntityManagerMock(typedQuery, graph, query);

        UserResource sat = new UserResource();
        ComponentInjector injector = new ComponentInjector(
                Map.of("converter", system.getComponent("converter"),
                        "validator", system.getComponent("validator"),
                        "config", system.getComponent("config")));
        injector.inject(sat);

        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setUserProfile("email", "kawasima0@example.com");

        UserProfileField emailField = builder(new UserProfileField())
                .set(UserProfileField::setId, 1L)
                .set(UserProfileField::setJsonName, "email")
                .set(UserProfileField::setNeedsVerification, true)
                .build();
        Mockito.when(typedQuery.getResultList()).thenReturn(
                List.of(emailField)
        );

        User user = builder(new User())
                .set(User::setAccount, "kawasima")
                .set(User::setUserProfileValues, new ArrayList<>(List.of(
                        builder(new UserProfileValue())
                                .set(UserProfileValue::setUserProfileField, emailField)
                                .set(UserProfileValue::setValue, "kawasima0@example.com")
                                .build()
                )))
                .build();
        ActionRecord actionRecord = new ActionRecord();
        UserPermissionPrincipal principal = new UserPermissionPrincipal(1L, "kawasima", Map.of("email", "kawasima0@example.com"), Set.of());
        RestContext context = new RestContext(new DefaultResource(), new DefaultHttpRequest());
        sat.update(updateRequest, user, actionRecord, principal, context, em);

        verify(em, never()).persist(eq(builder(new UserProfileVerification())
                .set(UserProfileVerification::setUser, user)
                .set(UserProfileVerification::setUserProfileField, emailField)
                .build()));
    }
}
