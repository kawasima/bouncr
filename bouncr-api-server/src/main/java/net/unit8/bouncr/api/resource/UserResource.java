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
import net.unit8.bouncr.api.service.UserProfileService;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileValue;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
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

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_USERS") || p.hasPermission("LIST_ANY_USERS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_USER") || p.hasPermission("MODIFY_ANY_USER"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("DELETE_USER") || p.hasPermission("DELETE_ANY_USER"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(UserUpdateRequest updateRequest, RestContext context, EntityManager em) {
        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(updateRequest);
        Problem problem = Problem.fromViolations(violations);

        UserProfileService userProfileService = new UserProfileService(em);
        Set<Problem.Violation> profileViolations = userProfileService.validateUserProfile(updateRequest.getUserProfiles());
        problem.getViolations().addAll(profileViolations);

        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        userRoot.fetch("userProfileValues", JoinType.LEFT);
        query.where(builder.equal(userRoot.get("account"), params.get("account")));

        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());

        EntityGraph<User> userGraph = em.createEntityGraph(User.class);
        userGraph.addAttributeNodes("account", "userProfileValues");
        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            userRoot.fetch("groups", JoinType.LEFT);
            query.distinct(true);
            userGraph.addAttributeNodes("groups");
            userGraph.addSubgraph("groups")
                    .addAttributeNodes("name", "description");
        }
        User user = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .getResultStream().findAny().orElse(null);

        if (user != null) {
            context.putValue(user);
        }
        return user != null;
    }

    @Decision(HANDLE_OK)
    public User handleOk(User user, Parameters params, EntityManager em) {
        return user;
    }

    @Decision(PUT)
    public User update(UserUpdateRequest updateRequest, User user, EntityManager em) {
        UserProfileService userProfileService = new UserProfileService(em);
        List<UserProfileValue> userProfileValues = userProfileService
                .convertToUserProfileValues(updateRequest.getUserProfiles());
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> {
            converter.copy(updateRequest, user);
            user.setUserProfileValues(userProfileValues);
        });
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