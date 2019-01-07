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
import net.unit8.bouncr.api.boundary.OidcApplicationCreateRequest;
import net.unit8.bouncr.api.boundary.OidcApplicationSearchParams;
import net.unit8.bouncr.entity.OidcApplication;

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
public class OidcApplicationsResrouce {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateSearchParams(Parameters params, RestContext context) {
        OidcApplicationSearchParams searchParams = converter.createFrom(params, OidcApplicationSearchParams.class);
        Set<ConstraintViolation<OidcApplicationSearchParams>> violations = validator.validate(searchParams);
        if (violations.isEmpty()) {
            context.putValue(searchParams);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(OidcApplicationCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<OidcApplicationCreateRequest>> violations = validator.validate(createRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_OIDC_APPLICATIONS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_OIDC_APPLICATION"))
                .isPresent();
    }

    @Decision(HANDLE_OK)
    public List<OidcApplication> list(OidcApplicationSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcApplication> query = cb.createQuery(OidcApplication.class);
        Root<OidcApplication> oidcApplicationRoot = query.from(OidcApplication.class);
        query.orderBy(cb.asc(oidcApplicationRoot.get("id")));
        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public OidcApplication create(OidcApplicationCreateRequest createRequest, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        OidcApplication oidcApplication = converter.createFrom(createRequest, OidcApplication.class);
        tx.required(() -> em.persist(oidcApplication));

        return oidcApplication;
    }
}
