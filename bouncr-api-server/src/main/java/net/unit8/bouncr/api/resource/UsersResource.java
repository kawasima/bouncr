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
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.UserCreateRequest;
import net.unit8.bouncr.api.boundary.UserSearchParams;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.service.UserProfileService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.entity.*;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class UsersResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateUserCreateRequest(UserCreateRequest createRequest, RestContext context, EntityManager em) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(createRequest);
        Problem problem = builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();

        config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, createRequest.getUserProfiles());
        UserProfileService userProfileService = new UserProfileService(em);
        Set<Problem.Violation> profileViolations = userProfileService.validateUserProfile(createRequest.getUserProfiles());
        problem.getViolations().addAll(profileViolations);

        if (problem.getViolations().isEmpty()) {
            context.putValue(createRequest);
        }

        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateUserSearchParams(Parameters params, RestContext context) {
        UserSearchParams userSearchParam = converter.createFrom(params, UserSearchParams.class);
        Set<ConstraintViolation<UserSearchParams>> violations = validator.validate(userSearchParam);
        if (violations.isEmpty()) {
            context.putValue(userSearchParam);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();

    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read") || p.hasPermission("any_user:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:create") || p.hasPermission("any_user:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean conflict(UserCreateRequest createRequest,
                            RestContext context,
                            EntityManager em) {
        UserProfileService userProfileService = new UserProfileService(em);
        Set<Problem.Violation> violations = userProfileService.validateAccountUniqueness(createRequest.getAccount());
        violations.addAll(userProfileService.validateProfileUniqueness(createRequest.getUserProfiles()));

        if (!violations.isEmpty()) {
            Problem problem = builder(Problem.valueOf(409))
                    .set(Problem::setType, BouncrProblem.CONFLICT.problemUri())
                    .build();
            problem.getViolations().addAll(violations);
            context.setMessage(problem);
        }
        return !violations.isEmpty();
    }

    @Decision(HANDLE_OK)
    public List<User> handleOk(UserSearchParams params, UserPermissionPrincipal principal, EntityManager em) {
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
        Subgraph<UserProfileValue> userProfileValuesGraph = userGraph.addSubgraph("userProfileValues");
        userProfileValuesGraph.addAttributeNodes("value", "userProfileField");
        userProfileValuesGraph.addSubgraph("userProfileField").addAttributeNodes("id", "name", "jsonName");

        List<Predicate> predicates = new ArrayList<>();
        if (params.getGroupId() != null) {
            Join<User, Group> groups = userRoot.join("groups");
            predicates.add(cb.equal(groups.get("id"), params.getGroupId()));
        }

        if (!principal.hasPermission("any_user:read")) {
            Join<User, ?> groups = userRoot.getJoins().stream().filter(j -> j.getModel().getBindableJavaType() == Group.class)
                    .findAny()
                    .orElseGet(() -> userRoot.join("groups"));
            Join users = groups.join("users");
            predicates.add(cb.notEqual(groups.get("name"), "BOUNCR_USER"));
            predicates.add(cb.equal(users.get("id"), principal.getId()));
        }

        Optional.ofNullable(params.getQ())
                .ifPresent(q -> {
                    String likeExpr = "%" + q.replaceAll("%", "_%") + "%";
                    predicates.add(cb.like(userRoot.get("account"), likeExpr, '_'));
                });

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            userGraph.addAttributeNodes("groups");
            userGraph.addSubgraph("groups").addAttributeNodes("name");
        }
        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public User doPost(UserCreateRequest createRequest,
                       ActionRecord actionRecord,
                       UserPermissionPrincipal principal,
                       RestContext context,
                       EntityManager em) {
        User user = converter.createFrom(createRequest, User.class);

        UserProfileService userProfileService = new UserProfileService(em);
        // Process user profiles
        List<UserProfileValue> userProfileValues = userProfileService
                .convertToUserProfileValues(createRequest.getUserProfiles());
        user.setUserProfileValues(userProfileValues.stream()
                .peek(v -> v.setUser(user))
                .collect(Collectors.toList()));
        // Process user profile verifications
        List<UserProfileVerification> profileVerifications = config.getVerificationPolicy().isVerificationEnabledAtCreateUser() ?
                userProfileService
                        .createProfileVerification(userProfileValues).stream()
                        .peek(v -> v.setUser(user))
                        .collect(Collectors.toList())
                :
                Collections.emptyList();

        user.setWriteProtected(false);
        context.putValue(user);

        config.getHookRepo().runHook(HookPoint.BEFORE_CREATE_USER, context);

        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> {
            em.persist(user);
            profileVerifications.forEach(em::persist);
            config.getHookRepo().runHook(HookPoint.AFTER_CREATE_USER, context);
        });
        actionRecord.setActionType(ActionType.USER_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(user.getAccount());
        return user;
    }
}
