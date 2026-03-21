package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OAuth2Error;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;

/**
 * OAuth2 Token Revocation endpoint (RFC 7009).
 * POST /oauth2/token/revoke (application/x-www-form-urlencoded)
 *
 * <p>Always returns 200 OK regardless of whether the token was valid.
 * Phase 2 implementation revokes authorization codes only.
 * JWT access token revocation (via blacklist) deferred to Phase 3.</p>
 */
@AllowedMethods("POST")
public class OAuth2TokenRevocationResource {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(POST)
    public boolean doPost() {
        return true;
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(Parameters params, HttpRequest request, DSLContext dsl) {
        // 1. Authenticate client (required per RFC 7009 §2.1)
        OAuth2ClientAuthenticator authenticator = new OAuth2ClientAuthenticator(config);
        boolean basicAuthAttempted = authenticator.hasBasicAuth(request);
        OAuth2ClientAuthenticator.AuthResult authResult = authenticator.authenticate(params, request, dsl);
        if (authResult == null) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed", basicAuthAttempted);
        }

        // 2. Validate token parameter (required per RFC 7009 §2.1)
        String token = params.get("token");
        if (token == null || token.isBlank()) {
            return tokenError(OAuth2Error.INVALID_REQUEST, "The 'token' parameter is required", basicAuthAttempted);
        }
        // Revoke — idempotent, always returns 200 even for unknown/expired tokens (RFC 7009 §2.2)
        storeProvider.getStore(AUTHORIZATION_CODE).delete(token);

        // 3. Return 200 OK with empty body
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store"))
                .set(ApiResponse::setBody, Map.of())
                .build();
    }

    private ApiResponse tokenError(OAuth2Error error, String description, boolean basicAuthAttempted) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) body.put("error_description", description);
        Headers headers = basicAuthAttempted && error == OAuth2Error.INVALID_CLIENT
                ? Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "WWW-Authenticate", "Basic realm=\"bouncr\"")
                : Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store");
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, headers)
                .set(ApiResponse::setBody, body)
                .build();
    }
}
