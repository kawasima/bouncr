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
import net.unit8.bouncr.api.boundary.OidcApplicationCreateRequest;
import net.unit8.bouncr.api.boundary.OidcApplicationSearchParams;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.OidcApplication;
import net.unit8.bouncr.entity.Permission;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.security.KeyPair;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class OidcApplicationsResoruce {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateSearchParams(Parameters params, RestContext context) {
        OidcApplicationSearchParams searchParams = converter.createFrom(params, OidcApplicationSearchParams.class);
        Set<ConstraintViolation<OidcApplicationSearchParams>> violations = validator.validate(searchParams);
        if (violations.isEmpty()) {
            context.putValue(searchParams);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(OidcApplicationCreateRequest createRequest, RestContext context) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<OidcApplicationCreateRequest>> violations = validator.validate(createRequest);
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
                .filter(p -> p.hasPermission("oidc_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(OidcApplicationCreateRequest createRequest, EntityManager em) {
        UniquenessCheckService<OidcApplication> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(OidcApplication.class, "nameLower",
                Optional.ofNullable(createRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(HANDLE_OK)
    public List<OidcApplication> list(OidcApplicationSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcApplication> query = cb.createQuery(OidcApplication.class);
        Root<OidcApplication> oidcApplicationRoot = query.from(OidcApplication.class);

        List<Predicate> predicates = new ArrayList<>();
        Optional.ofNullable(params.getQ())
                .ifPresent(q -> {
                    String likeExpr = "%" + q.replaceAll("%", "_%") + "%";
                    predicates.add(cb.like(oidcApplicationRoot.get("name"), likeExpr, '_'));
                });
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
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
        oidcApplication.setClientId(RandomUtils.generateRandomString(16, config.getSecureRandom()));
        oidcApplication.setClientSecret(RandomUtils.generateRandomString(32, config.getSecureRandom()));

        KeyPair keyPair = KeyUtils.generate(2048, config.getSecureRandom());
        oidcApplication.setPublicKey(keyPair.getPublic().getEncoded());
        oidcApplication.setPrivateKey(keyPair.getPrivate().getEncoded());

        if (createRequest.getPermissions() != null) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
            Root<Permission> root = query.from(Permission.class);
            query.where(cb.in(root.get("name").in(createRequest.getPermissions())));
            oidcApplication.setPermissions(em.createQuery(query).getResultList());
        }
        tx.required(() -> em.persist(oidcApplication));

        return oidcApplication;
    }
}
