package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import net.unit8.bouncr.api.boundary.AssignmentsRequest;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import static kotowari.restful.DecisionPoint.POST;

/**
 * Assignments Resource.
 *
 * This resource has
 *
 * <ul>
 *   <li>GET: list of assignments</li>
 *   <li>POST: Create multiple assignments</li>
 *   <li>POST: Create multiple assignments</li>
 * </ul>
 *
 * @author kawasima
 */
public class AssignmentsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(POST)
    public AssignmentsRequest doPost(AssignmentsRequest assignmentsRequest, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        return null;
    }

}
