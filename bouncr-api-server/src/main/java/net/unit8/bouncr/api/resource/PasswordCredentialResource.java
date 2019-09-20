package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.PasswordCredentialCreateRequest;
import net.unit8.bouncr.api.boundary.PasswordCredentialDeleteRequest;
import net.unit8.bouncr.api.boundary.PasswordCredentialUpdateRequest;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.service.PasswordPolicyService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.ActionType;
import net.unit8.bouncr.entity.PasswordCredential;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.config.HookPoint.*;

@AllowedMethods({"POST", "PUT", "DELETE"})
public class PasswordCredentialResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(PasswordCredentialCreateRequest createRequest, RestContext context, EntityManager em) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        PasswordPolicyService passwordPolicyService = new PasswordPolicyService(config.getPasswordPolicy(), em);
        Set<ConstraintViolation<PasswordCredentialCreateRequest>> violations = validator.validate(createRequest);
        Problem problem = builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
        Optional.ofNullable(passwordPolicyService.validateCreatePassword(createRequest))
                .ifPresent(violation -> problem.getViolations().add(violation));

        if (problem.getViolations().isEmpty()) {
            context.putValue(createRequest);
        }
        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(PasswordCredentialUpdateRequest updateRequest, RestContext context, EntityManager em) {
        if (updateRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        PasswordPolicyService passwordPolicyService = new PasswordPolicyService(config.getPasswordPolicy(), em);
        Set<ConstraintViolation<PasswordCredentialUpdateRequest>> violations = validator.validate(updateRequest);
        Problem problem = builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
        Optional.ofNullable(passwordPolicyService.validateUpdatePassword(updateRequest))
                .ifPresent(violation -> problem.getViolations().add(violation));

        if (problem.getViolations().isEmpty()) {
            context.putValue(updateRequest);
        }
        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(value = MALFORMED, method = "DELETE")
    public Problem validateDeleteRequest(PasswordCredentialUpdateRequest deleteRequest, RestContext context, EntityManager em) {
        if (deleteRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<PasswordCredentialUpdateRequest>> violations = validator.validate(deleteRequest);
        Problem problem = builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();

        if (problem.getViolations().isEmpty()) {
            context.putValue(deleteRequest);
        }
        return problem.getViolations().isEmpty() ? null : problem;
    }

    @Decision(value = AUTHORIZED, method = {"POST", "DELETE"})
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal, PasswordCredentialCreateRequest createRequest) {
        if (principal.hasPermission("any_user:update") || principal.hasPermission("user:update")) {
            return true;
        }
        return principal.getName().equals(createRequest.getAccount());
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal, PasswordCredentialUpdateRequest updateRequest) {
        if (principal == null) return true;
        return principal.getName().equals(updateRequest.getAccount());
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal, PasswordCredentialDeleteRequest deleteRequest) {
        if (principal.hasPermission("any_user:delete") || principal.hasPermission("user:delete")) {
            return true;
        }
        return principal.getName().equals(deleteRequest.getAccount());
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
                })
                .filter(user -> user.getPasswordCredential() != null)
                .filter(user ->
                        Arrays.equals(user.getPasswordCredential().getPassword(),
                                PasswordUtils.pbkdf2(updateRequest.getOldPassword(), user.getPasswordCredential().getSalt(), 100))
                )
                .isPresent();
    }

    @Decision(POST)
    public PasswordCredential create(PasswordCredentialCreateRequest createRequest,
                                     User user,
                                     UserPermissionPrincipal principal,
                                     ActionRecord actionRecord,
                                     RestContext context,
                                     EntityManager em) {
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        PasswordCredential passwordCredential = builder(new PasswordCredential())
                .set(PasswordCredential::setUser, user)
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, createRequest.isInitial())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(createRequest.getPassword(), salt, 100))
                .set(PasswordCredential::setCreatedAt, LocalDateTime.now())
                .build();

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.persist(passwordCredential);
            context.putValue(passwordCredential);
            config.getHookRepo().runHook(AFTER_CREATE_PASSWORD_CREDENTIAL, context);
        });

        actionRecord.setActionType(ActionType.PASSWORD_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(user.getAccount());

        em.detach(passwordCredential);
        return passwordCredential;
    }

    @Decision(PUT)
    public PasswordCredential update(PasswordCredentialUpdateRequest updateRequest,
                                     User user,
                                     UserPermissionPrincipal principal,
                                     ActionRecord actionRecord,
                                     RestContext context,
                                     EntityManager em) {

        PasswordCredential passwordCredential = user.getPasswordCredential();
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            passwordCredential.setSalt(salt);
            passwordCredential.setPassword(PasswordUtils.pbkdf2(updateRequest.getNewPassword(), salt, 100));
            passwordCredential.setInitial(false);
            passwordCredential.setCreatedAt(LocalDateTime.now());
            em.merge(passwordCredential);
            context.putValue(passwordCredential);
            config.getHookRepo().runHook(AFTER_UPDATE_PASSWORD_CREDENTIAL, context);
        });
        em.detach(passwordCredential);

        actionRecord.setActionType(ActionType.PASSWORD_CHANGED);
        actionRecord.setActor(Optional.ofNullable(principal)
                .map(Principal::getName)
                .orElse(user.getAccount()));
        actionRecord.setDescription(user.getAccount());

        return passwordCredential;
    }

    @Decision(DELETE)
    public Void delete(UserPermissionPrincipal principal,
                       ActionRecord actionRecord,
                       RestContext context,
                       EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PasswordCredential> query = cb.createQuery(PasswordCredential.class);
        Root<PasswordCredential> passwordCredentialRoot = query.from(PasswordCredential.class);
        Join<User, PasswordCredential> userRoot = passwordCredentialRoot.join("user");
        query.where(cb.equal(userRoot.get("name"), principal.getName()));
        EntityTransactionManager tx = new EntityTransactionManager(em);
        em.createQuery(query)
                .getResultStream()
                .findAny()
                .ifPresent(passwordCredential -> tx.required(() -> {
                    em.remove(passwordCredential);
                    config.getHookRepo().runHook(AFTER_DELETE_PASSWORD_CREDENTIAL, context);
                }));

        actionRecord.setActionType(ActionType.PASSWORD_DELETED);
        actionRecord.setActor(principal.getName());

        return null;
    }

}
