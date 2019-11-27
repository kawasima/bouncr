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
import net.unit8.bouncr.api.boundary.OidcProviderCreateRequest;
import net.unit8.bouncr.api.boundary.OidcProviderSearchParams;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.OidcProvider;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
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
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(OidcProviderCreateRequest createRequest, RestContext context) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<OidcProviderCreateRequest>> violations = validator.validate(createRequest);
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
    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(OidcProviderCreateRequest createRequest, EntityManager em) {
        UniquenessCheckService<OidcProvider> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(OidcProvider.class, "nameLower",
                Optional.ofNullable(createRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }
    @Decision(HANDLE_OK)
    public List<OidcProvider> list(OidcProviderSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcProvider> query = cb.createQuery(OidcProvider.class);
        Root<OidcProvider> oidcProviderRoot = query.from(OidcProvider.class);
        List<Predicate> predicates = new ArrayList<>();
        Optional.ofNullable(params.getQ())
                .ifPresent(q -> {
                    String likeExpr = "%" + q.replaceAll("%", "_%") + "%";
                    predicates.add(cb.like(oidcProviderRoot.get("name"), likeExpr, '_'));
                });
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        query.orderBy(cb.asc(oidcProviderRoot.get("id")));
        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList()
                .stream()
                .map(oidcProvider -> {
                    // Excludes client secret
                    oidcProvider.setClientSecret(null);
                    return oidcProvider;
                })
                .collect(Collectors.toList());
    }

    @Decision(POST)
    public OidcProvider create(OidcProviderCreateRequest createRequest, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        OidcProvider oidcProvider = converter.createFrom(createRequest, OidcProvider.class);
        tx.required(() -> em.persist(oidcProvider));

        return oidcProvider;
    }

}
