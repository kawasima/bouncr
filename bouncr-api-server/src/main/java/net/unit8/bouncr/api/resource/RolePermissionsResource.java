package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.RolePermissionsRequest;
import net.unit8.bouncr.entity.Permission;
import net.unit8.bouncr.entity.Role;
import net.unit8.bouncr.entity.User;

import jakarta.inject.Inject;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST", "DELETE"})
public class RolePermissionsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:read") || p.hasPermission("any_role:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= { "POST", "DELETE" })
    public boolean isModifyAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:update") || p.hasPermission("any_role:update"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> query = cb.createQuery(Role.class);
        Root<Role> roleRoot = query.from(Role.class);
        query.where(cb.equal(roleRoot.get("name"), params.get("name")));

        Role role = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (role != null) {
            context.putValue(role);
        }
        return role != null;
    }

    @Decision(HANDLE_OK)
    public List<Permission> list(Role role, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> userRoot = query.from(Permission.class);
        Join<Role, User> rolesJoin = userRoot.join("roles");
        query.where(cb.equal(rolesJoin.get("id"), role.getId()));
        query.orderBy(cb.asc(userRoot.get("id")));
        return em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultList();
    }

    @Decision(POST)
    public RolePermissionsRequest create(RolePermissionsRequest createRequest, Role role, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> permissionRoot = query.from(Permission.class);
        query.where(permissionRoot.get("name").in(createRequest));
        List<Permission> permissions = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultList();

        EntityTransactionManager tx = new EntityTransactionManager(em);

        tx.required(() -> {
            HashSet<Permission> rolePermissions = new HashSet<>(role.getPermissions());
            rolePermissions.addAll(permissions);
            role.setPermissions(new ArrayList<>(rolePermissions));
        });

        return createRequest;
    }

    @Decision(DELETE)
    public RolePermissionsRequest delete(RolePermissionsRequest deleteRequest, Role role, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> permissionRoot = query.from(Permission.class);
        query.where(permissionRoot.get("name").in(deleteRequest));
        List<Permission> permissions = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultList();

        EntityTransactionManager tx = new EntityTransactionManager(em);

        tx.required(() -> {
            HashSet<Permission> rolePermissions = new HashSet<>(role.getPermissions());
            rolePermissions.removeAll(permissions);
            role.setPermissions(new ArrayList<>(rolePermissions));
        });

        return deleteRequest;
    }

}
