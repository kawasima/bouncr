package net.unit8.bouncr.api.resource;

import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.PasswordCredentialCreateRequest;
import net.unit8.bouncr.api.boundary.PasswordCredentialUpdateRequest;
import net.unit8.bouncr.api.service.PasswordPolicyService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"POST", "PUT", "DELETE"})
public class PasswordCredentialResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(PasswordCredentialCreateRequest createRequest, RestContext context, EntityManager em) {
        PasswordPolicyService passwordPolicyService = new PasswordPolicyService(config.getPasswordPolicy(), em);
        Set<ConstraintViolation<PasswordCredentialCreateRequest>> violations = validator.validate(createRequest);
        Problem problem = Problem.fromViolations(violations);
        Optional.ofNullable(passwordPolicyService.validateCreatePassword(createRequest))
                .ifPresent(violation -> problem.getViolations().add(violation));

        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(PasswordCredentialUpdateRequest updateRequest, EntityManager em) {
        PasswordPolicyService passwordPolicyService = new PasswordPolicyService(config.getPasswordPolicy(), em);
        Set<ConstraintViolation<PasswordCredentialUpdateRequest>> violations = validator.validate(updateRequest);
        Problem problem = Problem.fromViolations(violations);
        Optional.ofNullable(passwordPolicyService.validateUpdatePassword(updateRequest))
                .ifPresent(violation -> problem.getViolations().add(violation));
        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean postAllowed(UserPermissionPrincipal principal, PasswordCredentialCreateRequest createRequest) {
        if (principal.hasPermission("CREATE_ANY_USER")) {
            return true;
        }
        return principal.getName().equals(createRequest.getAccount());
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean putAllowed(UserPermissionPrincipal principal, PasswordCredentialUpdateRequest createRequest) {
        if (principal.hasPermission("MODIFY_ANY_USER")) {
            return true;
        }
        return principal.getName().equals(createRequest.getAccount());
    }

    private Optional<User> findUserByAccount(String account, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(cb.equal(userRoot.get("account"), account));
        return em.createQuery(query).getResultStream().findAny();

    }

    @Decision(value=PROCESSABLE, method="POST")
    public boolean userProcessableInPost(PasswordCredentialCreateRequest createRequest, RestContext context, EntityManager em) {
        return findUserByAccount(createRequest.getAccount(), em)
                .map(user -> {
                    context.putValue(user);
                    return user;
                }).isPresent();
    }

    @Decision(value=PROCESSABLE, method="PUT")
    public boolean userProcessableInPut(PasswordCredentialUpdateRequest updateRequest, RestContext context, EntityManager em) {
        return findUserByAccount(updateRequest.getAccount(), em)
                .map(user -> {
                    context.putValue(user);
                    return user;
                }).isPresent();
    }

    @Decision(POST)
    public PasswordCredential create(PasswordCredentialCreateRequest createRequest, User user, EntityManager em) {
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        PasswordCredential passwordCredential = builder(new PasswordCredential())
                .set(PasswordCredential::setUser, user)
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, true)
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(createRequest.getPassword(), salt, 100))
                .set(PasswordCredential::setCreatedAt, LocalDateTime.now())
                .build();

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(passwordCredential));
        em.detach(passwordCredential);
        return passwordCredential;
    }

    @Decision(PUT)
    public PasswordCredential update(PasswordCredentialUpdateRequest updateRequest, UserPermissionPrincipal principal, HttpRequest request,
                                     EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PasswordCredential> passwordCredentialQuery = cb.createQuery(PasswordCredential.class);
        Root<PasswordCredential> passwordCredentialRoot = passwordCredentialQuery.from(PasswordCredential.class);
        Join<User, PasswordCredential> userJoin = passwordCredentialRoot.join("user");

        passwordCredentialQuery.where(cb.equal(userJoin.get("account"), principal.getName()));
        PasswordCredential passwordCredential = em.createQuery(passwordCredentialQuery)
                .getResultStream()
                .findAny()
                .orElseThrow();

        Arrays.equals(passwordCredential.getPassword(),
                PasswordUtils.pbkdf2(updateRequest.getOldPassword(), passwordCredential.getSalt(), 100));
        passwordCredential.setCreatedAt(LocalDateTime.now());

        UserAction userAction = builder(new UserAction())
                .set(UserAction::setActionType, ActionType.CHANGE_PASSWORD)
                .set(UserAction::setActor, principal.getName())
                .set(UserAction::setActorIp, request.getRemoteAddr())
                .set(UserAction::setCreatedAt, LocalDateTime.now())
                .build();
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.merge(passwordCredential);
            em.persist(userAction);
        });
        em.detach(passwordCredential);
        return passwordCredential;
    }

    @Decision(DELETE)
    public Void delete(UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OtpKey> query = cb.createQuery(OtpKey.class);
        Root<OtpKey> otpKeyRoot = query.from(OtpKey.class);
        Join<User, OtpKey> userRoot = otpKeyRoot.join("user");
        query.where(cb.equal(userRoot.get("name"), principal.getName()));
        EntityTransactionManager tx = new EntityTransactionManager(em);
        em.createQuery(query)
                .getResultStream()
                .findAny()
                .ifPresent(otpKey -> tx.required(() -> em.remove(otpKey)));
        return null;
    }

}
