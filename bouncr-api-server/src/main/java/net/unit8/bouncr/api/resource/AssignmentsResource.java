package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.AssignmentsRequest;
import net.unit8.bouncr.entity.Assignment;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Realm;
import net.unit8.bouncr.entity.Role;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.DELETE;
import static kotowari.restful.DecisionPoint.IS_AUTHORIZED;
import static kotowari.restful.DecisionPoint.POST;

/**
 * Assignments Resource.
 *
 * This resource has
 *
 * <ul>
 *   <li>GET: list of assignments</li>
 *   <li>POST: Create multiple assignments</li>
 *   <li>DELETE: Create multiple assignments</li>
 * </ul>
 *
 * @author kawasima
 */
@AllowedMethods({"POST", "DELETE"})
public class AssignmentsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(POST)
    public Void doPost(AssignmentsRequest assignmentsRequest, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            Assignment assignment = builder(new Assignment())
                    .set(Assignment::setGroup, em.find(Group.class, assignmentsRequest.getGroup().getId()))
                    .set(Assignment::setRole,  em.find(Role.class,  assignmentsRequest.getRole().getId()))
                    .set(Assignment::setRealm, em.find(Realm.class, assignmentsRequest.getRealm().getId()))
                    .build();
            em.persist(assignment);
        });
        return null;
    }

    @Decision(DELETE)
    public Void delete(AssignmentsRequest assignmentsRequest, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            Assignment assignment = builder(new Assignment())
                    .set(Assignment::setGroup, em.find(Group.class, assignmentsRequest.getGroup().getId()))
                    .set(Assignment::setRole,  em.find(Role.class, assignmentsRequest.getRole().getId()))
                    .set(Assignment::setRealm, em.find(Realm.class, assignmentsRequest.getRealm().getId()))
                    .build();
            em.remove(assignment);
        });
        return null;
    }
}
