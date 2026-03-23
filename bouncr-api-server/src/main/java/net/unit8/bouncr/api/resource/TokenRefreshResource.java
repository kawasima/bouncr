package net.unit8.bouncr.api.resource;

import enkan.data.HttpRequest;
import enkan.exception.MisconfigurationException;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.TokenRefresh;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.api.service.SignatureVerifier;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
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
    static final ContextKey<TokenRefresh> REFRESH_REQ = ContextKey.of(TokenRefresh.class);
    static final ContextKey<HashMap> PROFILE = ContextKey.of("profile", HashMap.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "session_id is required", BouncrProblem.MALFORMED.problemUri());
        }
        return switch (BouncrJsonDecoders.TOKEN_REFRESH.decode(body)) {
            case Ok<TokenRefresh> ok -> { context.put(REFRESH_REQ, ok.value()); yield null; }
            case Err<TokenRefresh>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean authorized(HttpRequest request, TokenRefresh refreshReq) {
        String key = config.getInternalSigningKey();
        if (key == null || key.isBlank()) {
            throw new MisconfigurationException("bouncr.INTERNAL_SIGNING_KEY_REQUIRED");
        }
        String signature = request.getHeaders().get(SignatureVerifier.HEADER);
        SignatureVerifier verifier = new SignatureVerifier(key);
        return verifier.verify(signature, refreshReq.sessionId());
    }

    @Decision(POST)
    public boolean doPost(TokenRefresh refreshReq, RestContext context, DSLContext dsl) {
        Serializable refreshData = storeProvider.getStore(REFRESH_TOKEN).read(refreshReq.sessionId());
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

        SignInService signInService = new SignInService(dsl, storeProvider, config);
        HashMap<String, Object> profileMap = signInService.refreshAccessToken(userId, refreshReq.sessionId());
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
