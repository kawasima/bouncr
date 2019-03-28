package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.InitialPassword;
import net.unit8.bouncr.api.boundary.PasswordResetRequest;
import net.unit8.bouncr.api.service.PasswordCredentialService;
import net.unit8.bouncr.api.service.UserLockService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.entity.PasswordResetChallenge;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"PUT"})
public class PasswordResetResource {
    @Inject
    private BouncrConfiguration config;
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validate(PasswordResetRequest createRequest) {
        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(createRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(PROCESSABLE)
    public boolean existsCode(PasswordResetRequest resetRequest,
                                 RestContext context,
                                 EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PasswordResetChallenge> query = cb.createQuery(PasswordResetChallenge.class);
        Root<PasswordResetChallenge> userRoot = query.from(PasswordResetChallenge.class);
        query.where(cb.equal(userRoot.get("code"), resetRequest.getCode()));
        PasswordResetChallenge resetChallenge = em.createQuery(query).getResultStream().findAny().orElse(null);

        if (resetChallenge != null) {
            context.putValue(resetChallenge);
            context.putValue(resetChallenge.getUser());
        }
        return resetChallenge != null;
    }

    @Decision(PUT)
    public Void reset(PasswordResetChallenge resetChallenge,
                      User user,
                      RestContext context,
                      EntityManager em) {
        PasswordCredentialService passwordCredentialService = new PasswordCredentialService(em, config);
        UserLockService userLockService = new UserLockService(em, config);

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.remove(resetChallenge);
            userLockService.unlockUser(user);
            InitialPassword initialPassword = passwordCredentialService.initializePassword(user);
            context.putValue(initialPassword);
        });
        config.getHookRepo().runHook(HookPoint.AFTER_PASSWORD_RESET, context);

        return null;
    }
}
