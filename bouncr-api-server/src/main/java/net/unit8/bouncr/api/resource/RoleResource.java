package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.RoleUpdateRequest;
import net.unit8.bouncr.entity.Role;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class RoleResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = IS_ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_ROLES") || p.hasPermission("LIST_ANY_ROLES"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_ANY_ROLE") || p.hasPermission("MODIFY_ROLE"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("DELETE_ANY_ROLE") || p.hasPermission("DELETE_ROLE"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> query = cb.createQuery(Role.class);
        Root<Role> roleRoot = query.from(Role.class);
        query.where(cb.equal(roleRoot.get("name"), params.get("name")));

        Role role = em.createQuery(query).getResultStream().findAny().orElse(null);
        if (role != null) {
            context.putValue(role);
        }
        return role != null;
    }

    @Decision(HANDLE_OK)
    public Role handleOk(Role role) {
        return role;
    }

    @Decision(PUT)
    public Role update(RoleUpdateRequest updateRequest, Role role, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, role));
        em.detach(role);
        return role;
    }

    @Decision(DELETE)
    public Void delete(Role role, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(role));
        return null;
    }
}
