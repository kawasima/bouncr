package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.service.OidcClaimMapper;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

/**
 * OIDC UserInfo endpoint (OpenID Connect Core §5.3).
 * GET/POST /oauth2/userinfo
 *
 * Returns user claims based on the granted scopes in the Bearer access_token.
 */
@AllowedMethods({"GET", "POST"})
public class OAuth2UserInfoResource {
    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(HANDLE_OK)
    public ApiResponse handleOk(HttpRequest request, DSLContext dsl) {
        // Extract Bearer token
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return errorResponse(401, "invalid_token", "Bearer token required");
        }
        String accessToken = authHeader.substring(7);

        // Decode payload without verification to extract client_id for key lookup
        Map<String, Object> unverifiedPayload = RsaJwtSigner.extractUnverifiedClaims(accessToken);
        if (unverifiedPayload == null) {
            return errorResponse(401, "invalid_token", "Malformed token");
        }

        Object clientIdObj = unverifiedPayload.get("client_id");
        if (!(clientIdObj instanceof String clientId)) {
            return errorResponse(401, "invalid_token", "Token missing or invalid client_id claim");
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

        // Check expiry (required claim)
        long now = config.getClock().instant().getEpochSecond();
        Object expObj = claims.get("exp");
        if (!(expObj instanceof Number)) {
            return errorResponse(401, "invalid_token", "Token missing exp claim");
        }
        if (((Number) expObj).longValue() < now) {
            return errorResponse(401, "invalid_token", "Token expired");
        }

        // Build UserInfo response
        Object subObj = claims.get("sub");
        if (!(subObj instanceof String sub)) {
            return errorResponse(401, "invalid_token", "Token missing sub claim");
        }
        String scope = claims.get("scope") instanceof String s ? s : null;

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("sub", sub);
        OidcClaimMapper.addUserClaimsByAccount(userInfo, sub, scope, dsl);

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, userInfo)
                .build();
    }

    private ApiResponse errorResponse(int status, String error, String description) {
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, status)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache",
                        "WWW-Authenticate", "Bearer error=\"" + error + "\", error_description=\"" + description + "\""))
                .set(ApiResponse::setBody, Map.of("error", error, "error_description", description))
                .build();
    }
}
