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
import net.unit8.bouncr.api.boundary.OidcProviderUpdateRequest;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.OidcProvider;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcProviderResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateUpdateRequest(OidcProviderUpdateRequest updateRequest, RestContext context) {
        if (updateRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<OidcProviderUpdateRequest>> violations = validator.validate(updateRequest);
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(Parameters params, OidcProviderUpdateRequest updateRequest, EntityManager em) {
        if (Objects.equals(params.get("name"), updateRequest.getName())) {
            return false;
        }

        UniquenessCheckService<OidcProvider> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(OidcProvider.class, "nameLower",
                Optional.ofNullable(updateRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcProvider> query = cb.createQuery(OidcProvider.class);
        Root<OidcProvider> oidcProviderRoot = query.from(OidcProvider.class);
        query.where(cb.equal(oidcProviderRoot.get("name"), params.get("name")));
        OidcProvider oidcProvider = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (oidcProvider != null) {
            context.putValue(oidcProvider);
        }
        return oidcProvider != null;
    }

    @Decision(HANDLE_OK)
    public OidcProvider find(OidcProvider oidcProvider) {
        // Excludes a client secret
        oidcProvider.setClientSecret(null);
        return oidcProvider;
    }

    @Decision(PUT)
    public OidcProvider update(OidcProviderUpdateRequest updateRequest, OidcProvider oidcProvider, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, oidcProvider));
        return oidcProvider;
    }

    @Decision(DELETE)
    public Void delete(OidcProvider oidcProvider, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(oidcProvider));
        em.detach(oidcProvider);
        return null;
    }

}
