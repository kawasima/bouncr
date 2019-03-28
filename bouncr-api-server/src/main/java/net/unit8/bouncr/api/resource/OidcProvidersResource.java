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
import net.unit8.bouncr.api.boundary.OidcProviderCreateRequest;
import net.unit8.bouncr.api.boundary.OidcProviderSearchParams;
import net.unit8.bouncr.entity.OidcApplication;
import net.unit8.bouncr.entity.OidcProvider;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class OidcProvidersResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateSearchParams(Parameters params, RestContext context) {
        OidcProviderSearchParams searchParams = converter.createFrom(params, OidcProviderSearchParams.class);
        Set<ConstraintViolation<OidcProviderSearchParams>> violations = validator.validate(searchParams);
        if (violations.isEmpty()) {
            context.putValue(searchParams);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(OidcProviderCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<OidcProviderCreateRequest>> violations = validator.validate(createRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
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
    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:create"))
                .isPresent();
    }

    @Decision(HANDLE_OK)
    public List<OidcProvider> list(OidcProviderSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcProvider> query = cb.createQuery(OidcProvider.class);
        Root<OidcProvider> oidcProviderRoot = query.from(OidcProvider.class);
        query.orderBy(cb.asc(oidcProviderRoot.get("id")));
        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public OidcProvider create(OidcProviderCreateRequest createRequest, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        OidcProvider oidcProvider = converter.createFrom(createRequest, OidcProvider.class);
        tx.required(() -> em.persist(oidcProvider));

        return oidcProvider;
    }

}
