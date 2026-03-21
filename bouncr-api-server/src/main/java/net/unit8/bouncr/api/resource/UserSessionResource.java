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

import jakarta.inject.Inject;
import java.util.Map;
import java.util.Objects;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.REFRESH_TOKEN;

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

    @SuppressWarnings("unchecked")
    @Decision(EXISTS)
    public boolean exists(Parameters params, UserPermissionPrincipal principal, RestContext context) {
        String token = params.get("token");
        if (token == null) {
            return false;
        }

        Map<String, Object> profiles = (Map<String, Object>) storeProvider.getStore(BOUNCR_TOKEN).read(token);
        if (profiles == null) {
            return false;
        }

        return Objects.equals(profiles.get("sub"), principal.getName());
    }

    @Decision(DELETE)
    public Void delete(Parameters params, RestContext context) {
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_OUT, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return null;
        }
        String token = params.get("token");
        storeProvider.getStore(BOUNCR_TOKEN).delete(token);
        storeProvider.getStore(REFRESH_TOKEN).delete(token);

        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_OUT, context);
        return null;
    }
}
