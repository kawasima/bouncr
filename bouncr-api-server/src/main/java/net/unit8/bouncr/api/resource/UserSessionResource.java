package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserSession;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;

/**
 * A User Session Resource.
 *
 * Deleting an user session means sign out.
 *
 * @Author kawasima
 */
@AllowedMethods("DELETE")
public class UserSessionResource {
    @Inject
    private StoreProvider storeProvider;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, UserPermissionPrincipal principal, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserSession> query = cb.createQuery(UserSession.class);
        Root<UserSession> userSessionRoot = query.from(UserSession.class);
        Join<User, UserSession> userJoin = userSessionRoot.join("user");
        query.where(cb.equal(userJoin.get("id"), principal.getId()),
                cb.equal(userSessionRoot.get("token"), params.get("token")));

        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream()
                .findAny()
                .map(s -> { context.putValue(s); return s; })
                .isPresent();
    }

    @Decision(DELETE)
    public Void delete(UserSession userSession, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        storeProvider.getStore(BOUNCR_TOKEN).delete(userSession.getToken());

        tx.required(() -> em.remove(userSession));
        return null;
    }
}
