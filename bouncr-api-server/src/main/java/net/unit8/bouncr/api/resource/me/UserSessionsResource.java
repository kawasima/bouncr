package net.unit8.bouncr.api.resource.me;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.boundary.UserSessionSearchParams;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserSession;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Set;

import static kotowari.restful.DecisionPoint.HANDLE_OK;
import static kotowari.restful.DecisionPoint.MALFORMED;

public class UserSessionsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(MALFORMED)
    public Problem validate(Parameters params, RestContext context) {
        UserSessionSearchParams userSessionSearchParams = converter.createFrom(params, UserSessionSearchParams.class);
        Set<ConstraintViolation<UserSessionSearchParams>> violations = validator.validate(userSessionSearchParams);
        if (violations.isEmpty()) {
            context.putValue(userSessionSearchParams);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public List<UserSession> handleOk(UserSessionSearchParams params, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserSession> query = cb.createQuery(UserSession.class);
        Root<UserSession> userSessionRoot = query.from(UserSession.class);
        Join<User, UserSession> userJoin = userSessionRoot.join("user");
        query.where(cb.equal(userJoin.get("name"), principal.getName()));

        return em.createQuery(query)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }
}
