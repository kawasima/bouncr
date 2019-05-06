package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.entity.Assignment;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Realm;
import net.unit8.bouncr.entity.Role;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET"})
public class AssignmentResource {
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
                .filter(p -> p.hasPermission("assignments:read"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Assignment> query = cb.createQuery(Assignment.class);
        Root<Assignment> root = query.from(Assignment.class);
        Join<Assignment, Group> groupJoin = root.join("group");
        Join<Assignment, Role> roleJoin = root.join("role");
        Join<Assignment, Realm> realmJoin = root.join("realm");
        query.where(
                cb.equal(groupJoin.get("name"), params.get("group")),
                cb.equal(roleJoin.get("name"), params.get("role")),
                cb.equal(realmJoin.get("name"), params.get("realm")));

        Assignment assignment = em.createQuery(query).getResultStream().findAny().orElse(null);
        if (assignment != null) {
            context.putValue(assignment);
        }
        return assignment != null;
    }

    @Decision(HANDLE_OK)
    public Assignment find(Assignment assignment) {
        return assignment;
    }
}
