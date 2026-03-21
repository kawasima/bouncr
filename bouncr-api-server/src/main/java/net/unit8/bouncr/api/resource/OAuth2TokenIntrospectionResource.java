package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

/**
 * OAuth2 Token Introspection endpoint (RFC 7662).
 * POST /oauth2/token/introspect (application/x-www-form-urlencoded)
 *
 * Allows resource servers to validate access tokens by verifying the JWT
 * signature and returning the token's claims.
 */
@AllowedMethods("POST")
public class OAuth2TokenIntrospectionResource {
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
        // 1. Authenticate client (required per RFC 7662 §2.1)
        OAuth2ClientAuthenticator authenticator = new OAuth2ClientAuthenticator(config);
        boolean basicAuthAttempted = authenticator.hasBasicAuth(request);
        OAuth2ClientAuthenticator.AuthResult authResult = authenticator.authenticate(params, request, dsl);
        if (authResult == null) {
            return errorResponse(OAuth2Error.INVALID_CLIENT, "Client authentication failed", basicAuthAttempted);
        }

        // 2. Get token to introspect (required per RFC 7662 §2.1)
        String token = params.get("token");
        if (token == null || token.isBlank()) {
            return errorResponse(OAuth2Error.INVALID_REQUEST, "The 'token' parameter is required", basicAuthAttempted);
        }

        // 3. Decode unverified payload to extract client_id for key lookup
        Map<String, Object> unverifiedPayload = RsaJwtSigner.extractUnverifiedClaims(token);
        if (unverifiedPayload == null) {
            return inactiveResponse();
        }

        Object clientIdObj = unverifiedPayload.get("client_id");
        if (!(clientIdObj instanceof String tokenClientId)) {
            return inactiveResponse();
        }

        // Only allow introspection of tokens issued to the authenticated client
        if (!tokenClientId.equals(authResult.app().clientId())) {
            return inactiveResponse();
        }

        // 4. Load public key and verify signature
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        byte[] publicKey = repo.findPublicKeyByClientId(tokenClientId).orElse(null);
        if (publicKey == null) {
            return inactiveResponse();
        }

        Map<String, Object> claims = RsaJwtSigner.verify(token, publicKey);
        if (claims == null) {
            return inactiveResponse();
        }

        // 5. Check expiry
        long now = config.getClock().instant().getEpochSecond();
        Object expObj = claims.get("exp");
        if (!(expObj instanceof Number) || ((Number) expObj).longValue() < now) {
            return inactiveResponse();
        }

        // 6. Build active response (RFC 7662 §2.2)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", true);
        if (claims.get("sub") != null) response.put("sub", claims.get("sub"));
        if (claims.get("client_id") != null) response.put("client_id", claims.get("client_id"));
        if (claims.get("scope") != null) response.put("scope", claims.get("scope"));
        if (claims.get("iss") != null) response.put("iss", claims.get("iss"));
        if (claims.get("exp") != null) response.put("exp", claims.get("exp"));
        if (claims.get("iat") != null) response.put("iat", claims.get("iat"));
        if (claims.get("jti") != null) response.put("jti", claims.get("jti"));
        response.put("token_type", "Bearer");

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, response)
                .build();
    }

    private ApiResponse inactiveResponse() {
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, Map.of("active", false))
                .build();
    }

    private ApiResponse errorResponse(OAuth2Error error, String description, boolean basicAuthAttempted) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) body.put("error_description", description);
        Headers headers = basicAuthAttempted && error == OAuth2Error.INVALID_CLIENT
                ? Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store", "Pragma", "no-cache",
                        "WWW-Authenticate", "Basic realm=\"bouncr\"")
                : Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store", "Pragma", "no-cache");
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, headers)
                .set(ApiResponse::setBody, body)
                .build();
    }
}
