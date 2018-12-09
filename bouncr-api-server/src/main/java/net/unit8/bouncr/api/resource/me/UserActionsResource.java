package net.unit8.bouncr.api.resource.me;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import net.unit8.bouncr.entity.UserAction;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.HANDLE_OK;
import static kotowari.restful.DecisionPoint.IS_AUTHORIZED;

public class UserActionsResource {
    @Decision(IS_AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(HANDLE_OK)
    public List<UserAction> handleOk(UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserAction> userActionQuery = cb.createQuery(UserAction.class);
        Root<UserAction> userActionRoot = userActionQuery.from(UserAction.class);
        return em.createQuery(userActionQuery)
                .getResultList();
    }
}
