package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.api.boundary.UserUpdateRequest;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class UserResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(UserUpdateRequest updateRequest, RestContext context) {
        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(updateRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = IS_ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_USERS") || p.hasPermission("LIST_ANY_USERS"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_USER") || p.hasPermission("MODIFY_ANY_USER"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("DELETE_USER") || p.hasPermission("DELETE_ANY_USER"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(builder.equal(userRoot.get("account"), params.get("account")));

        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());

        EntityGraph<User> userGraph = em.createEntityGraph(User.class);
        userGraph.addAttributeNodes("name", "email", "account", "writeProtected");
        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            userGraph.addSubgraph("groups").addAttributeNodes("name", "description");
        }

        User user = em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .getResultStream().findAny().orElse(null);

        if (user != null) {
            context.putValue(user);
        }
        return user != null;
    }

    @Decision(HANDLE_OK)
    public User handleOk(User user, Parameters params, EntityManager em) {
        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        if (!embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            user.setGroups(null);
        } else {
            user.getGroups().stream().forEach(g -> g.setUsers(null));
        }
        return user;
    }

    @Decision(PUT)
    public User update(UserUpdateRequest updateRequest, User user, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> converter.copy(updateRequest, user));
        em.detach(user);
        return user;
    }

    @Decision(DELETE)
    public Void delete(User user, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> em.remove(user));
        em.detach(user);
        return null;
    }
}
