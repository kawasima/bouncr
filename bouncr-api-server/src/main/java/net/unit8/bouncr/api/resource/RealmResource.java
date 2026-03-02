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
import net.unit8.bouncr.api.boundary.RealmUpdateRequest;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.Application;
import net.unit8.bouncr.entity.Realm;

import jakarta.inject.Inject;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ConstraintViolation;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class RealmResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem vaidateUpdateRequest(RealmUpdateRequest updateRequest, RestContext context) {
        if (updateRequest == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        Set<ConstraintViolation<RealmUpdateRequest>> violations = validator.validate(updateRequest);
        if (violations.isEmpty()) {
            context.putValue(updateRequest);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }
    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:read") || p.hasPermission("any_realm:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:update") || p.hasPermission("any_realm:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:delete") || p.hasPermission("any_realm:delete"))
                .isPresent();
    }

    @Decision(PROCESSABLE)
    public boolean isProcessable(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Application> query = cb.createQuery(Application.class);
        Root<Application> applicationRoot = query.from(Application.class);
        query.where(cb.equal(applicationRoot.get("name"), params.get("name")));

        Application application = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (application != null) {
            context.putValue(application);
        }
        return application != null;
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(RealmUpdateRequest updateRequest, Parameters params, EntityManager em) {
        if (Objects.equals(updateRequest.getName(), params.get("name"))) {
            return false;
        }
        UniquenessCheckService<Realm> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(Realm.class, "nameLower",
                Optional.ofNullable(updateRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, Application application, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Realm> query = cb.createQuery(Realm.class);
        Root<Realm> realmRoot = query.from(Realm.class);
        Join<Application, Realm> applicationJoin = realmRoot.join("application");
        query.where(cb.equal(realmRoot.get("name"), params.get("realmName")),
                cb.equal(applicationJoin.get("id"), application.getId()));

        Realm realm = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (realm != null) {
            context.putValue(realm);
        }
        return realm != null;
    }

    @Decision(HANDLE_OK)
    public Realm handleOk(Realm realm, EntityManager em) {
        em.detach(realm);
        return realm;
    }

    @Decision(PUT)
    public Realm update(RealmUpdateRequest updateRequest, Realm realm, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, realm));
        em.detach(realm);
        return realm;
    }

    @Decision(DELETE)
    public Void delete(Realm realm, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(realm));
        return null;
    }

}
