package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.PasswordResetChallengeCreateRequest;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.entity.PasswordResetChallenge;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

/**
 * Password Reset.
 *
 * Anonymous user can call this endpoint.
 */
@AllowedMethods({"GET", "POST"})
public class PasswordResetChallengeResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(PasswordResetChallengeCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<PasswordResetChallengeCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(createRequest);
            config.getHookRepo().runHook(HookPoint.BEFORE_PASSWORD_RESET_CHALLENGE, context);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(PROCESSABLE)
    public boolean existsAccount(PasswordResetChallengeCreateRequest createRequest,
                                 RestContext context,
                                 EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(cb.equal(userRoot.get("account"), createRequest.getAccount()));
        User user = em.createQuery(query).getResultStream().findAny().orElse(null);

        if (user != null) {
            context.putValue(user);
        }
        return user != null;
    }

    @Decision(HANDLE_UNPROCESSABLE_ENTITY)
    public Problem unprocessable() {
        return Problem.valueOf(422, "Account not found");
    }

    /**
     * Create a code for reset password.
     *
     * User can only know the code by email or SMS.
     * So this endpoint returns Void.
     *
     * @param createRequest a creation request for the password reset challenge
     * @param user an user entity
     * @param context an REST context
     * @param em an entity manager
     * @return null
     */
    @Decision(POST)
    public Void create(PasswordResetChallengeCreateRequest createRequest,
                                         User user,
                                         RestContext context,
                                         EntityManager em) {
        PasswordResetChallenge passwordResetChallenge = converter.createFrom(createRequest, PasswordResetChallenge.class);

        passwordResetChallenge.setUser(user);
        passwordResetChallenge.setCode(RandomUtils.generateRandomString(8));
        passwordResetChallenge.setExpiresAt(LocalDateTime.now()
                .plus(Duration.ofMinutes(120))); // TODO configuration

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(passwordResetChallenge));
        context.putValue(user);
        context.putValue(passwordResetChallenge);

        config.getHookRepo().runHook(HookPoint.AFTER_PASSWORD_RESET_CHALLENGE, context);

        return null;
    }
}
