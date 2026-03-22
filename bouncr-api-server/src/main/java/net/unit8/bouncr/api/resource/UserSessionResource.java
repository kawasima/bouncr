package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.service.OidcLogoutService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
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
    static final ContextKey<String> SUBJECT = ContextKey.of("subject", String.class);
    @SuppressWarnings("unchecked")
    static final ContextKey<Map<String, Object>> LOGOUT_RESULT = (ContextKey<Map<String, Object>>) (ContextKey<?>) ContextKey.of("logoutResult", Map.class);

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

        if (Objects.equals(profiles.get("sub"), principal.getName())) {
            context.put(SUBJECT, principal.getName());
            return true;
        }
        return false;
    }

    @Decision(ALLOWED)
    public boolean allowed(RestContext context) {
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_OUT, context);
        return !context.getMessage().filter(Problem.class::isInstance).isPresent();
    }

    @Decision(DELETE)
    public Void delete(Parameters params, String subject, RestContext context, DSLContext dsl) {
        String token = params.get("token");
        String resolvedSubject = resolveSubject(subject, token);
        OidcLogoutService.LogoutResult logoutResult = new OidcLogoutService(config).propagateSignOut(resolvedSubject, dsl);
        storeProvider.getStore(BOUNCR_TOKEN).delete(token);
        storeProvider.getStore(REFRESH_TOKEN).delete(token);

        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_OUT, context);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("attempted", logoutResult.backchannelLogout().attempted());
        summary.put("succeeded", logoutResult.backchannelLogout().succeeded());
        summary.put("failed", logoutResult.backchannelLogout().failed());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("frontchannel_logout_urls", List.copyOf(logoutResult.frontchannelLogoutUrls()));
        response.put("backchannel_logout", summary);
        context.put(LOGOUT_RESULT, response);
        return null;
    }

    @Decision(RESPOND_WITH_ENTITY)
    public boolean respondWithEntity() {
        return true;
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> handleOk(RestContext context) {
        return context.get(LOGOUT_RESULT).orElse(Map.of());
    }

    @SuppressWarnings("unchecked")
    private String resolveSubject(String subject, String token) {
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        if (token == null) {
            return null;
        }

        Map<String, Object> profiles = (Map<String, Object>) storeProvider.getStore(BOUNCR_TOKEN).read(token);
        if (profiles == null || profiles.get("sub") == null) {
            return null;
        }
        return String.valueOf(profiles.get("sub"));
    }
}
