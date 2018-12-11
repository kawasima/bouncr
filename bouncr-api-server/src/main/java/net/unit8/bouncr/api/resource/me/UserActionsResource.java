package net.unit8.bouncr.api.resource.me;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.UserActionSearchParams;
import net.unit8.bouncr.entity.UserAction;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Set;

import static kotowari.restful.DecisionPoint.HANDLE_OK;
import static kotowari.restful.DecisionPoint.IS_AUTHORIZED;
import static kotowari.restful.DecisionPoint.MALFORMED;

@AllowedMethods("GET")
public class UserActionsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(IS_AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(MALFORMED)
    public Problem validateUserActionSearch(Parameters params, RestContext context) {
        UserActionSearchParams userActionSearchParams = converter.createFrom(params, UserActionSearchParams.class);
        Set<ConstraintViolation<UserActionSearchParams>> violations = validator.validate(userActionSearchParams);
        if (violations.isEmpty()) {
            context.putValue(userActionSearchParams);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public List<UserAction> handleOk(UserActionSearchParams params, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserAction> query = cb.createQuery(UserAction.class);
        Root<UserAction> userActionRoot = query.from(UserAction.class);
        query.where(cb.equal(userActionRoot.get("actor"), principal.getName()));
        query.orderBy(cb.desc(userActionRoot.get("createdAt")));

        return em.createQuery(query)
                .setMaxResults(params.getLimit())
                .getResultList();
    }
}
