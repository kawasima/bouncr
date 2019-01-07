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
import net.unit8.bouncr.api.boundary.UserCreateRequest;
import net.unit8.bouncr.api.boundary.UserSearchParams;
import net.unit8.bouncr.api.service.UserProfileService;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.entity.UserProfileVerification;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class UsersResource {
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

    @Decision(value = ALLOWED, method= "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_USER") || p.hasPermission("CREATE_ANY_USER"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateUserCreateRequest(UserCreateRequest createRequest, RestContext context, EntityManager em) {
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(createRequest);
        Problem problem = Problem.fromViolations(violations);

        UserProfileService userProfileService = new UserProfileService(em);
        Set<Problem.Violation> profileViolations = userProfileService.validateUserProfile(createRequest.getUserProfiles());
        problem.getViolations().addAll(profileViolations);

        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateUserSearchParams(Parameters params, RestContext context) {
        UserSearchParams userSearchParam = converter.createFrom(params, UserSearchParams.class);
        Set<ConstraintViolation<UserSearchParams>> violations = validator.validate(userSearchParam);
        if (violations.isEmpty()) {
            context.putValue(userSearchParam);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);

    }

    @Decision(HANDLE_OK)
    public List<User> handleOk(UserSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        query.distinct(true);
        Root<User> userRoot = query.from(User.class);
        userRoot.fetch("userProfileValues");
        query.orderBy(cb.asc(userRoot.get("id")));

        List<ResourceField> embedEntities = some(params.getEmbed(), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<User> userGraph = em.createEntityGraph(User.class);
        userGraph.addAttributeNodes("account", "userProfileValues");
        if (params.getGroupId() != null) {
            Join<Group, User> groups = userRoot.join("groups");
            query.where(cb.equal(groups.get("id"), params.getGroupId()));
        }
        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            userGraph.addAttributeNodes("groups");
            userGraph.addSubgraph("groups").addAttributeNodes("name");
        }
        List<User> resultList = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
        return resultList;
    }

    @Decision(POST)
    public User doPost(UserCreateRequest createRequest, EntityManager em) {
        User user = converter.createFrom(createRequest, User.class);

        UserProfileService userProfileService = new UserProfileService(em);
        // Process user profiles
        List<UserProfileValue> userProfileValues = userProfileService
                .convertToUserProfileValues(createRequest.getUserProfiles());
        user.setUserProfileValues(userProfileValues.stream()
                .map(v -> { v.setUser(user); return v; })
                .collect(Collectors.toList()));
        // Process user profile verifications
        List<UserProfileVerification> profileVerifications = userProfileService
                .createProfileVerification(userProfileValues).stream()
                .map(v -> {
                    v.setUser(user);
                    return v;
                })
                .collect(Collectors.toList());

        user.setWriteProtected(false);
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> {
            em.persist(user);
            profileVerifications.forEach(em::persist);
        });
        return user;
    }
}
