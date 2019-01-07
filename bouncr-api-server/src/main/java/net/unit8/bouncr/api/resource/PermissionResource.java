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
import net.unit8.bouncr.api.boundary.PermissionUpdateRequest;
import net.unit8.bouncr.entity.Permission;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Optional;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class PermissionResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(PermissionUpdateRequest updateRequest, RestContext context) {
        Set<ConstraintViolation<PermissionUpdateRequest>> violations = validator.validate(updateRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_PERMISSIONS") || p.hasPermission("LIST_ANY_PERMISSIONS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_PERMISSION") || p.hasPermission("MODIFY_ANY_PERMISSION"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("DELETE_PERMISSION") || p.hasPermission("DELETE_ANY_PERMISSION"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> permissionRoot = query.from(Permission.class);
        query.where(cb.equal(permissionRoot.get("name"), params.get("name")));

        Permission permission = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (permission != null) {
            context.putValue(permission);
        }
        return permission != null;
    }

    @Decision(HANDLE_OK)
    public Permission find(Permission permission, EntityManager em) {
        em.detach(permission);
        return permission;
    }

    @Decision(PUT)
    public Permission update(PermissionUpdateRequest updateRequest, Permission permission, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, permission));
        em.detach(permission);
        return permission;
    }

    @Decision(DELETE)
    public Void delete(Permission permission, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(permission));
        return null;
    }

}
