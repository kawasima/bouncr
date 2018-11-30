package net.unit8.bouncr.web.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.web.boundary.UpdateUserRequest;
import net.unit8.bouncr.web.entity.User;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;

import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class UserResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(builder.equal(userRoot.get("account"), params.get("account")));
        User user = em.createQuery(query).getSingleResult();
        context.putValue(user);
        return user != null;
    }

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(UpdateUserRequest updateRequest, RestContext context) {
        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(updateRequest);
        context.putValue(updateRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public User handleOk(User user) {
        return user;
    }

    @Decision(PUT)
    public User update(UpdateUserRequest updateRequest, User user, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> converter.copy(updateRequest, user));
        return user;
    }

    @Decision(DELETE)
    public User delete(User user, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> em.remove(user));
        return user;
    }
}
