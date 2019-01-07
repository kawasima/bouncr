package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.PasswordSignInRequest;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.util.PasswordUtils;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.entity.ActionType.USER_SIGNIN;

@AllowedMethods("POST")
public class PasswordSignInResource {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    protected UserAction createUserAction(User user, HttpRequest request, String account) {
        return builder(new UserAction())
                .set(UserAction::setActionType, user != null ? USER_SIGNIN : USER_FAILED_SIGNIN)
                .set(UserAction::setActor, account)
                .set(UserAction::setActorIp, request.getRemoteAddr())
                .set(UserAction::setCreatedAt, LocalDateTime.now())
                .build();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validatePasswordSignInRequest(PasswordSignInRequest passwordSignInRequest, RestContext context) {
        Set<ConstraintViolation<PasswordSignInRequest>> violations = validator.validate(passwordSignInRequest);
        if (violations.isEmpty()) {
            context.putValue(passwordSignInRequest);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(AUTHORIZED)
    public boolean authenticate(PasswordSignInRequest passwordSignInRequest,
                                HttpRequest request,
                                UserPermissionPrincipal principal,
                                RestContext context,
                                final EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> userQuery = cb.createQuery(User.class);
        Root<User> userRoot = userQuery.from(User.class);
        userQuery.where(cb.equal(userRoot.get("account"), passwordSignInRequest.getAccount()));
        User user = em.createQuery(userQuery)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);

        Problem problem = null;
        if (user != null) {
            PasswordCredential credential = em.find(PasswordCredential.class, user.getId());
            if (credential != null && Arrays.equals(
                    credential.getPassword(),
                    PasswordUtils.pbkdf2(passwordSignInRequest.getPassword(), credential.getSalt(), 100))) {
                context.putValue(user);
            } else {
                // Password doesn't match
                problem = new Problem(null, "Authentication failed", 401, null, null);
            }
        } else {
            // User not found
            problem = new Problem(null, "Authentication failed", 401, null, null);
        }
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.persist(createUserAction(user, request, passwordSignInRequest.getAccount()));
        });
        if (user != null) {
            CriteriaQuery<UserAction> userActionCriteria = cb.createQuery(UserAction.class);
            Root<UserAction> userActionRoot = userActionCriteria.from(UserAction.class);
            userActionCriteria.where(userActionRoot.get("actionType").in(USER_SIGNIN, USER_FAILED_SIGNIN));
            if (em.createQuery(userActionCriteria)
                    .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                    .setMaxResults(config.getPasswordPolicy().getNumOfTrialsUntilLock())
                    .getResultStream()
                    .allMatch(action -> action.getActionType() == USER_FAILED_SIGNIN)) {
                UserLock userLock = em.find(UserLock.class, user.getId());
                if (userLock == null) {
                    em.persist(builder(new UserLock())
                            .set(UserLock::setUser, user)
                            .set(UserLock::setLockedAt, LocalDateTime.now())
                            .build());
                }
            }
        }

        return problem == null;
    }

    @Decision(POST)
    public UserSession doPost(User user, HttpRequest request, RestContext context, EntityManager em) {
        String token = UUID.randomUUID().toString();

        String userAgent = some(request.getHeaders().get("User-Agent"),
                ua -> ua.substring(0, Math.min(ua.length(), 255))).orElse("");
        final UserSession userSession = builder(new UserSession())
                .set(UserSession::setToken, token)
                .set(UserSession::setUser, user)
                .set(UserSession::setRemoteAddress, request.getRemoteAddr())
                .set(UserSession::setUserAgent, userAgent)
                .set(UserSession::setCreatedAt, LocalDateTime.now())
                .build();

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.persist(userSession);
        });
        HashMap<String, Object> profileMap = new HashMap<>(user.getUserProfileValues()
                .stream()
                .collect(Collectors.toMap(v -> v.getUserProfileField().getJsonName(), UserProfileValue::getValue)));
        profileMap.put("iss", "bouncr");
        profileMap.put("uid", Long.toString(user.getId()));
        profileMap.put("sub", user.getAccount());
        profileMap.put("permissionsByRealm", getPermissionsByRealm(user, em));

        storeProvider.getStore(BOUNCR_TOKEN).write(token, profileMap);
        context.putValue(userSession);
        return userSession;
    }

    @Decision(HANDLE_CREATED)
    public UserSession handleCreated(UserSession userSession) {
        return userSession;
    }


    protected Map<Long, Set<String>> getPermissionsByRealm(User user, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Assignment> assignmentCriteria = cb.createQuery(Assignment.class);
        Root<Assignment> assignmentRoot = assignmentCriteria.from(Assignment.class);
        Join<Group, Assignment> groupJoin = assignmentRoot.join("group");
        Join<User, Group> userJoin = groupJoin.join("users");
        assignmentRoot.fetch("role").fetch("permissions");
        assignmentCriteria.where(cb.equal(userJoin.get("id"), user.getId()));

        EntityGraph<Assignment> assignmentGraph = em.createEntityGraph(Assignment.class);
        assignmentGraph.addAttributeNodes("realm", "role");
        Subgraph<Role> roleGraph = assignmentGraph.addSubgraph("role");
        roleGraph.addAttributeNodes("permissions");
        Subgraph<Permission> permissionsGraph = roleGraph.addSubgraph("permissions");
        permissionsGraph.addAttributeNodes("name");

        return em.createQuery(assignmentCriteria)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", assignmentGraph)
                .getResultStream()
                .collect(Collectors.groupingBy(Assignment::getRealm))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getId(),
                        e -> e.getValue().stream()
                                .flatMap(v -> v.getRole().getPermissions().stream())
                                .map(p -> p.getName())
                                .collect(Collectors.toSet())));
    }

}
