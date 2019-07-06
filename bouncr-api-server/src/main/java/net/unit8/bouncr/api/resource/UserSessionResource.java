package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Map;
import java.util.Objects;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;

/**
 * A User Session Resource.
 *
 * Deleting an user session means sign out.
 *
 * @author kawasima
 */
@AllowedMethods("DELETE")
public class UserSessionResource {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, UserPermissionPrincipal principal, RestContext context, EntityManager em) {
        String token = params.get("token");
        if (token == null) {
            return false;
        }

        Map<String, Object> profiles = (Map<String, Object>)storeProvider.getStore(BOUNCR_TOKEN).read(token);
        if (profiles == null) {
            return false;
        }

        return Objects.equals(profiles.get("sub"), principal.getName());
        /*
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
                */
    }

    @Decision(DELETE)
    public Void delete(Parameters params, RestContext context, EntityManager em) {
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_OUT, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return null;
        }
        storeProvider.getStore(BOUNCR_TOKEN).delete(params.get("token"));
        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_OUT, context);

        /*
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(userSession));
        */
        return null;
    }
}
