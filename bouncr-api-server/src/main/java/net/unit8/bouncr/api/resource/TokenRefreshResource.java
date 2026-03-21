package net.unit8.bouncr.api.resource;

import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.REFRESH_TOKEN;

/**
 * Internal endpoint called by bouncr-proxy to refresh an expired access token.
 * <p>
 * When the short-lived BOUNCR_TOKEN key expires in Redis, the proxy calls this
 * endpoint with the session_id. This resource checks if the long-lived
 * BOUNCR_REFRESH key still exists, rebuilds the profileMap from DB with latest
 * permissions, and writes it back to BOUNCR_TOKEN.
 */
@AllowedMethods("POST")
public class TokenRefreshResource {
    static final ContextKey<String> SESSION_ID = ContextKey.of("sessionId", String.class);
    static final ContextKey<HashMap> PROFILE = ContextKey.of("profile", HashMap.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null || !body.has("session_id") || body.get("session_id").asString().isBlank()) {
            return Problem.valueOf(400, "session_id is required", BouncrProblem.MALFORMED.problemUri());
        }
        context.put(SESSION_ID, body.get("session_id").asString());
        return null;
    }

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(POST)
    public boolean doPost(String sessionId, RestContext context, DSLContext dsl) {
        // Check if the refresh token (long-lived session marker) still exists
        Serializable refreshData = storeProvider.getStore(REFRESH_TOKEN).read(sessionId);
        if (refreshData == null) {
            context.setMessage(Problem.valueOf(401, "Session expired", BouncrProblem.SESSION_EXPIRED.problemUri()));
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshMap = (Map<String, Object>) refreshData;
        Object userIdObj = refreshMap.get("userId");
        long userId;
        if (userIdObj instanceof Number n) {
            userId = n.longValue();
        } else {
            context.setMessage(Problem.valueOf(401, "Invalid refresh data", BouncrProblem.SESSION_EXPIRED.problemUri()));
            return false;
        }

        // Rebuild profileMap from DB (latest permissions)
        SignInService signInService = new SignInService(dsl, storeProvider, config);
        HashMap<String, Object> profileMap = signInService.refreshAccessToken(userId, sessionId);
        if (profileMap == null) {
            context.setMessage(Problem.valueOf(401, "User not found", BouncrProblem.SESSION_EXPIRED.problemUri()));
            return false;
        }

        context.put(PROFILE, profileMap);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Decision(HANDLE_CREATED)
    public HashMap<String, Object> handleCreated(HashMap profile) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("profile", profile);
        return response;
    }
}
