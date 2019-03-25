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
import net.unit8.bouncr.api.boundary.PermissionCreateRequest;
import net.unit8.bouncr.api.boundary.PermissionSearchParams;
import net.unit8.bouncr.entity.Permission;

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
public class PermissionsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:read") || p.hasPermission("any_permission:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:create") || p.hasPermission("any_permission:create"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validatePermissionCreateRequest(PermissionCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<PermissionCreateRequest>> violations = validator.validate(createRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validatePermissionSearchParams(Parameters params, RestContext context) {
        PermissionSearchParams permissionSearchParams = converter.createFrom(params, PermissionSearchParams.class);
        Set<ConstraintViolation<PermissionSearchParams>> violations = validator.validate(permissionSearchParams);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(permissionSearchParams, PermissionSearchParams.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(PermissionCreateRequest createRequest, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> permissionRoot = query.from(Permission.class);
        query.where(cb.equal(permissionRoot.get("name"), createRequest.getName()));
        return !em.createQuery(query)
                .getResultList()
                .isEmpty();
    }

    @Decision(HANDLE_OK)
    public List<Permission> list(PermissionSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> permissionRoot = query.from(Permission.class);
        query.orderBy(cb.asc(permissionRoot.get("id")));

        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public Permission create(PermissionCreateRequest createRequest, EntityManager em) {
        Permission permission = converter.createFrom(createRequest, Permission.class);
        permission.setWriteProtected(false);

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(permission));
        em.detach(permission);
        return permission;
    }
}
