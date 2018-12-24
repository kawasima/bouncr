package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.GroupUpdateRequest;
import net.unit8.bouncr.entity.Group;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class GroupResource {
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
                .filter(p -> p.hasPermission("LIST_GROUPS") || p.hasPermission("LIST_ANY_GROUPS"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_GROUP") || p.hasPermission("MODIFY_ANY_GROUP"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("DELETE_GROUP") || p.hasPermission("DELETE_ANY_GROUP"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> permissionRoot = query.from(Group.class);
        query.where(cb.equal(permissionRoot.get("name"), params.get("name")));

        Group group = em.createQuery(query).getResultStream().findAny().orElse(null);
        if (group != null) {
            context.putValue(group);
        }
        return group != null;
    }

    @Decision(HANDLE_OK)
    public Group find(Group group, EntityManager em) {
        em.detach(group);
        return group;
    }

    @Decision(PUT)
    public Group update(GroupUpdateRequest updateRequest, Group group, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, group));
        em.detach(group);
        return group;
    }

    @Decision(DELETE)
    public Void delete(Group group, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(group));
        return null;
    }
}
