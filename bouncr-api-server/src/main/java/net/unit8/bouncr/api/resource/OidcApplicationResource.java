package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.exception.UnreachableException;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.OidcApplicationUpdateRequest;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.OidcApplication;

import jakarta.inject.Inject;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ConstraintViolation;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcApplicationResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateUpdateRequest(OidcApplicationUpdateRequest updateRequest, RestContext context) {
        if (updateRequest == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        Set<ConstraintViolation<OidcApplicationUpdateRequest>> violations = validator.validate(updateRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(OidcApplicationUpdateRequest updateRequest, EntityManager em) {
        UniquenessCheckService<OidcApplication> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(OidcApplication.class, "nameLower",
                Optional.ofNullable(updateRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcApplication> query = cb.createQuery(OidcApplication.class);
        Root<OidcApplication> oidcApplicationRoot = query.from(OidcApplication.class);
        query.where(cb.equal(oidcApplicationRoot.get("name"), params.get("name")));
        OidcApplication oidcApplication = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (oidcApplication != null) {
            context.putValue(oidcApplication);
        }
        return oidcApplication != null;
    }

    @Decision(HANDLE_OK)
    public OidcApplication find(OidcApplication oidcApplication) {
        return oidcApplication;
    }

    @Decision(PUT)
    public OidcApplication update(OidcApplicationUpdateRequest updateRequest, OidcApplication oidcApplication, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, oidcApplication));
        return oidcApplication;
    }

    @Decision(DELETE)
    public Void delete(OidcApplication oidcApplication, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(oidcApplication));
        em.detach(oidcApplication);
        return null;
    }
}
