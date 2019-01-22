package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.api.boundary.GroupUpdateRequest;
import net.unit8.bouncr.entity.Group;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class GroupResource {
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
                .filter(p -> p.hasPermission("LIST_GROUPS") || p.hasPermission("LIST_ANY_GROUPS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_GROUP") || p.hasPermission("MODIFY_ANY_GROUP"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
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

        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<Group> groupGraph = em.createEntityGraph(Group.class);
        groupGraph.addAttributeNodes("name", "description");

        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("users"))) {
            groupGraph.addAttributeNodes("users");
            groupGraph.addSubgraph("users")
                    .addAttributeNodes("account");
        }

        Group group = em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", groupGraph)
                .getResultStream().findAny().orElse(null);
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
