package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.PasswordSignInRequest;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.api.service.UserLockService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserSession;
import net.unit8.bouncr.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.EXPIRED;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.INITIAL;
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
        if (passwordSignInRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<PasswordSignInRequest>> violations = validator.validate(passwordSignInRequest);
        if (violations.isEmpty()) {
            context.putValue(passwordSignInRequest);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
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
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_IN, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return false;
        }
        SignInService signInService = new SignInService(em, storeProvider, config);
        UserLockService userLockService = new UserLockService(em, config);

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
                    context.setMessage(builder(Problem.valueOf(401,"Password must be changed"))
                            .set(Problem::setType, BouncrProblem.PASSWORD_MUST_BE_CHANGED.problemUri())
                            .build());
                    return false;
                }

                if (!signInService.validateOtpKey(user.getOtpKey(), passwordSignInRequest.getOneTimePassword())) {
                    context.setMessage(builder(Problem.valueOf(401, "One time password is needed"))
                            .set(Problem::setType, BouncrProblem.ONE_TIME_PASSWORD_IS_NEEDED.problemUri())
                            .build());
                    return false;
                }
                return true;
            } else {
                actionRecord.setActionType(USER_FAILED_SIGNIN);
                userLockService.lockUser(user);
            }
        }
        return false;
    }

    @Decision(POST)
    public UserSession doPost(User user, HttpRequest request, RestContext context, EntityManager em) {
        SignInService signInService = new SignInService(em, storeProvider, config);
        String token = signInService.createToken();
        UserSession userSession = signInService.createUserSession(request, user, token);
        context.putValue(userSession);
        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_IN, context);
        return userSession;
    }

    @Decision(HANDLE_CREATED)
    public UserSession handleCreated(UserSession userSession) {
        return userSession;
    }


}
