package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.data.HttpRequest;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.PasswordSignInRequest;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.EXPIRED;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.INITIAL;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.entity.ActionType.USER_SIGNIN;

@AllowedMethods("POST")
public class PasswordSignInResource {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordSignInResource.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validatePasswordSignInRequest(PasswordSignInRequest passwordSignInRequest, RestContext context) {
        Set<ConstraintViolation<PasswordSignInRequest>> violations = validator.validate(passwordSignInRequest);
        if (violations.isEmpty()) {
            context.putValue(passwordSignInRequest);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    /**
     * Authenticate the given request.
     *
     * <p>Authentication flow</p>
     * <ul>
     *     <li>
     *         Find an user by the given account
     *         <ul>
     *             <li>
     *                 Found
     *                 <ul>
     *                     <li>Check whether account is locked</li>
     *                     <li>Check whether the given password matches the registered password</li>
     *                     <li>Check whether the given code matches OTP password</li>
     *                 </ul>
     *             </li>
     *             <li>
     *                 Not found
     *                 <ul>
     *                     <li>Fail to sign in</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>Logging the request of sign in</li>
     * </ul>
     *
     * @param passwordSignInRequest A request to sign in
     * @param actionRecord an action record
     * @param context REST context
     * @param em Entity manager
     * @return Whether request is authenticated successfully
     */
    @Decision(AUTHORIZED)
    public boolean authenticate(PasswordSignInRequest passwordSignInRequest,
                                ActionRecord actionRecord,
                                RestContext context,
                                final EntityManager em) {
        SignInService signInService = new SignInService(em, config);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(cb.equal(userRoot.get("account"), passwordSignInRequest.getAccount()));
        EntityGraph<User> userGraph = em.createEntityGraph(User.class);
        userGraph.addAttributeNodes("account", "userProfileValues", "otpKey", "userLock");
        userGraph.addSubgraph("otpKey").addAttributeNodes("key");
        userGraph.addSubgraph("userLock").addAttributeNodes("lockedAt");
        userGraph.addSubgraph("passwordCredential")
                .addAttributeNodes("password", "salt", "initial", "createdAt");

        User user = em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);

        Problem problem = null;
        if (user != null && user.getUserLock() != null) {
            context.setMessage(new Problem(URI.create("abount:blank"), "Authentication failed", 401,
                    "Account is locked", null));
            return false;
        }

        // Check if the given password matches the registered password
        if (user != null) {
            actionRecord.setActor(user.getAccount());
            if (user.getPasswordCredential() != null &&
                    Arrays.equals(
                            user.getPasswordCredential().getPassword(),
                            PasswordUtils.pbkdf2(
                                    passwordSignInRequest.getPassword(),
                                    user.getPasswordCredential().getSalt(),
                                    100))) {
                context.putValue(user);
                actionRecord.setActionType(USER_SIGNIN);
                SignInService.PasswordCredentialStatus status = signInService.validatePasswordCredentialAttributes(user);
                if (status == EXPIRED || status == INITIAL) {
                    context.setMessage(new Problem(null, "Authentication Success but...", 401, "Password must be changed", null));
                    return false;
                }
                return true;
            } else {
                actionRecord.setActionType(USER_FAILED_SIGNIN);
                signInService.lockUser(user);
            }
        }
        return false;
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

        /*
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.persist(userSession);
        });
        */
        HashMap<String, Object> profileMap = new HashMap<>(user.getUserProfileValues()
                .stream()
                .collect(Collectors.toMap(v -> v.getUserProfileField().getJsonName(), UserProfileValue::getValue)));
        profileMap.put("iss", "bouncr");
        profileMap.put("uid", Long.toString(user.getId()));
        profileMap.put("sub", user.getAccount());
        profileMap.put("permissionsByRealm", getPermissionsByRealm(user, em));
        LOG.debug("signIn profileMap = {}", profileMap);
        storeProvider.getStore(BOUNCR_TOKEN).write(token, profileMap);
        context.putValue(userSession);
        return userSession;
    }

    @Decision(HANDLE_CREATED)
    public UserSession handleCreated(UserSession userSession) {
        return userSession;
    }


    protected Map<String, List<String>> getPermissionsByRealm(User user, EntityManager em) {
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
                        e -> e.getKey().getId().toString(),
                        e -> new ArrayList<>(e.getValue().stream()
                                .flatMap(v -> v.getRole().getPermissions().stream())
                                .map(p -> p.getName())
                                .collect(Collectors.toSet()))));
    }

}
