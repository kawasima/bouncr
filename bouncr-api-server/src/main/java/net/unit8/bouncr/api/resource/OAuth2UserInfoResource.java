package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

/**
 * OIDC UserInfo endpoint (OpenID Connect Core §5.3).
 * GET /oauth2/userinfo
 *
 * Returns user claims based on the granted scopes in the Bearer access_token.
 */
@AllowedMethods({"GET", "POST"})
public class OAuth2UserInfoResource {
    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true; // Bearer token validation is done in HANDLE_OK
    }

    @Decision(HANDLE_OK)
    public ApiResponse handleOk(HttpRequest request, DSLContext dsl) {
        // Extract Bearer token
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return errorResponse(401, "invalid_token", "Bearer token required");
        }
        String accessToken = authHeader.substring(7);

        // Decode JWT header to find kid, then extract iss to determine client_id
        String[] parts = accessToken.split("\\.", 3);
        if (parts.length != 3) {
            return errorResponse(401, "invalid_token", "Malformed token");
        }

        // Decode payload without verification first to get iss (which contains client_id)
        Map<String, Object> unverifiedPayload;
        try {
            byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> p = tools.jackson.databind.json.JsonMapper.builder().build()
                    .readValue(payloadBytes, Map.class);
            unverifiedPayload = p;
        } catch (Exception e) {
            return errorResponse(401, "invalid_token", "Cannot decode token");
        }

        // Extract client_id from iss: .../oauth2/openid/<client_id>
        String iss = (String) unverifiedPayload.get("iss");
        String clientId = (String) unverifiedPayload.get("client_id");
        if (iss == null || clientId == null) {
            return errorResponse(401, "invalid_token", "Token missing required claims");
        }

        // Load public key and verify signature
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        byte[] publicKey = repo.findPublicKeyByClientId(clientId).orElse(null);
        if (publicKey == null) {
            return errorResponse(401, "invalid_token", "Unknown client");
        }

        Map<String, Object> claims = RsaJwtSigner.verify(accessToken, publicKey);
        if (claims == null) {
            return errorResponse(401, "invalid_token", "Signature verification failed");
        }

        // Check expiry
        long now = config.getClock().instant().getEpochSecond();
        Object expObj = claims.get("exp");
        if (expObj instanceof Number exp && exp.longValue() < now) {
            return errorResponse(401, "invalid_token", "Token expired");
        }

        // Build UserInfo response based on granted scopes
        String scope = (String) claims.get("scope");
        Set<String> scopes = scope != null
                ? new HashSet<>(Arrays.asList(scope.split("\\s+")))
                : Set.of();
        String sub = (String) claims.get("sub");

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("sub", sub);

        // Load user profile if profile/email scopes are granted
        if (scopes.contains("profile") || scopes.contains("email")) {
            UserRepository userRepo = new UserRepository(dsl);
            userRepo.findByAccount(sub).ifPresent(user -> {
                var profileValues = userRepo.loadProfileValues(user.id());
                for (var pv : profileValues) {
                    String jsonName = pv.userProfileField().jsonName();
                    if (scopes.contains("email") && "email".equals(jsonName)) {
                        userInfo.put("email", pv.value());
                    }
                    if (scopes.contains("profile")) {
                        if ("name".equals(jsonName) || "family_name".equals(jsonName)
                                || "given_name".equals(jsonName) || "preferred_username".equals(jsonName)) {
                            userInfo.put(jsonName, pv.value());
                        }
                    }
                }
            });
        }

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store"))
                .set(ApiResponse::setBody, userInfo)
                .build();
    }

    private ApiResponse errorResponse(int status, String error, String description) {
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, status)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "WWW-Authenticate", "Bearer error=\"" + error + "\", error_description=\"" + description + "\""))
                .set(ApiResponse::setBody, Map.of("error", error, "error_description", description))
                .build();
    }
}
