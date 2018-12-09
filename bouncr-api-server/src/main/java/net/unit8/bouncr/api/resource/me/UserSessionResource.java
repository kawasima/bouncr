package net.unit8.bouncr.api.resource.me;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserSession;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import static kotowari.restful.DecisionPoint.DELETE;
import static kotowari.restful.DecisionPoint.EXISTS;

public class UserSessionResource {
    @Decision(EXISTS)
    public boolean exists(Parameters params, UserPermissionPrincipal principal, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserSession> query = cb.createQuery(UserSession.class);
        Root<UserSession> userSessionRoot = query.from(UserSession.class);
        Join<User, UserSession> userJoin = userSessionRoot.join("user");
        query.where(cb.equal(userJoin.get("name"), principal.getName()),
                cb.equal(userSessionRoot.get("id"), params.getLong("userSessionId")));

        return em.createQuery(query).getResultStream()
                .findAny()
                .map(s -> { context.putValue(s); return s; })
                .isPresent();
    }

    @Decision(DELETE)
    public Void delete(UserSession userSession, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(userSession));
        return null;
    }
}
