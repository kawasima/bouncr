package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;

import static enkan.util.BeanBuilder.builder;
import net.unit8.bouncr.api.boundary.SignOutResponse;
import net.unit8.bouncr.api.service.OidcLogoutService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.REFRESH_TOKEN;

/**
 * A User Session Resource.
 *
 * Deleting an user session means sign out.
 * The special token value {@code "me"} resolves the session from the request Cookie,
 * enabling browser clients to sign out without holding the raw token in JavaScript.
 *
 * @author kawasima
 */
@AllowedMethods("DELETE")
public class UserSessionResource {
    static final ContextKey<String> SUBJECT = ContextKey.of("subject", String.class);
    static final ContextKey<String> RESOLVED_TOKEN = ContextKey.of("resolvedToken", String.class);
    static final ContextKey<SignOutResponse> LOGOUT_RESULT = ContextKey.of(SignOutResponse.class);

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
    public boolean exists(Parameters params, HttpRequest request, UserPermissionPrincipal principal, RestContext context) {
        String token = resolveToken(params.get("token"), request);
        if (token == null) {
            return false;
        }

        Map<String, Object> profiles = (Map<String, Object>) storeProvider.getStore(BOUNCR_TOKEN).read(token);
        if (profiles == null) {
            return false;
        }

        if (Objects.equals(profiles.get("sub"), principal.getName())) {
            context.put(SUBJECT, principal.getName());
            context.put(RESOLVED_TOKEN, token);
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
    public Void delete(String subject, RestContext context, DSLContext dsl) {
        String token = context.get(RESOLVED_TOKEN).orElse(null);
        String resolvedSubject = resolveSubject(subject, token);
        OidcLogoutService.LogoutResult logoutResult = new OidcLogoutService(config).propagateSignOut(resolvedSubject, dsl);
        storeProvider.getStore(BOUNCR_TOKEN).delete(token);
        storeProvider.getStore(REFRESH_TOKEN).delete(token);

        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_OUT, context);

        SignOutResponse response = new SignOutResponse(
                List.copyOf(logoutResult.frontchannelLogoutUrls()),
                new SignOutResponse.BackchannelLogoutSummary(
                        logoutResult.backchannelLogout().attempted(),
                        logoutResult.backchannelLogout().succeeded(),
                        logoutResult.backchannelLogout().failed()));
        context.put(LOGOUT_RESULT, response);
        return null;
    }

    @Decision(RESPOND_WITH_ENTITY)
    public boolean respondWithEntity() {
        return true;
    }

    @Decision(HANDLE_OK)
    public ApiResponse handleOk(RestContext context) {
        SignOutResponse body = context.get(LOGOUT_RESULT).orElse(new SignOutResponse(
                List.of(), new SignOutResponse.BackchannelLogoutSummary(0, 0, 0)));
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of("Set-Cookie", buildClearCookie()))
                .set(ApiResponse::setBody, body)
                .build();
    }

    @Decision(HANDLE_NOT_FOUND)
    public ApiResponse handleNotFound() {
        // Session not found (e.g. stale cookie after server restart). Still clear the cookie
        // so the browser is no longer stuck in an authenticated-but-invalid state.
        Problem body = Problem.valueOf(404, "Session not found");
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 404)
                .set(ApiResponse::setHeaders, Headers.of("Set-Cookie", buildClearCookie()))
                .set(ApiResponse::setBody, body)
                .build();
    }

    private String buildClearCookie() {
        return config.getTokenName() + "=; HttpOnly"
                + (config.isSecureCookie() ? "; Secure" : "")
                + "; SameSite=Strict; Max-Age=0; Path=/";
    }

    /**
     * Resolves the raw token value.
     * When {@code tokenParam} is {@code "me"}, reads the token from the session cookie.
     */
    private String resolveToken(String tokenParam, HttpRequest request) {
        if (tokenParam == null) {
            return null;
        }
        if ("me".equals(tokenParam)) {
            return some(request.getCookies().get(config.getTokenName()), Cookie::getValue).orElse(null);
        }
        return tokenParam;
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
