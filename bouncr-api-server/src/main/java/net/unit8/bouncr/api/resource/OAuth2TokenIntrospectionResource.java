package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.IntrospectionRequest;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.sign.RsaJwtSigner;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

/**
 * OAuth2 Token Introspection endpoint (RFC 7662).
 * {@code POST /oauth2/token/introspect} ({@code application/x-www-form-urlencoded})
 *
 * <p>Allows the token-issuing client to validate its own access tokens.
 * Cross-client introspection is not permitted.
 */
@AllowedMethods("POST")
public class OAuth2TokenIntrospectionResource {
    static final ContextKey<IntrospectionRequest> INTROSPECT_REQ =
            ContextKey.of("introspectReq", IntrospectionRequest.class);
    static final ContextKey<OidcApplication> CLIENT_APP = ContextKey.of(OidcApplication.class);
    static final ContextKey<Boolean> BASIC_AUTH_ATTEMPTED = ContextKey.of("basicAuthAttempted", Boolean.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem isMalformed(Parameters params, RestContext context) {
        return switch (BouncrFormDecoders.INTROSPECTION_REQUEST.decode(params)) {
            case Ok<IntrospectionRequest> ok -> {
                context.put(INTROSPECT_REQ, ok.value());
                yield null;
            }
            case Err<IntrospectionRequest> err -> {
                List<Problem.Violation> violations = err.issues().asList().stream()
                        .map(issue -> new Problem.Violation(
                                issue.path().toString(), issue.code(), issue.message()))
                        .toList();
                yield Problem.fromViolationList(violations);
            }
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(Parameters params, HttpRequest request,
                                RestContext context, DSLContext dsl) {
        OAuth2ClientAuthenticator authenticator = new OAuth2ClientAuthenticator(config);
        context.put(BASIC_AUTH_ATTEMPTED, authenticator.hasBasicAuth(request));
        OAuth2ClientAuthenticator.AuthResult authResult = authenticator.authenticate(params, request, dsl);
        if (authResult == null) return false;
        context.put(CLIENT_APP, authResult.app());
        return true;
    }

    @Decision(HANDLE_UNAUTHORIZED)
    public ApiResponse handleUnauthorized(Boolean basicAuthAttempted) {
        boolean basic = basicAuthAttempted != null && basicAuthAttempted;
        Headers headers = basic
                ? Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store", "Pragma", "no-cache",
                        "WWW-Authenticate", "Basic realm=\"bouncr\"")
                : Headers.of("Content-Type", "application/json",
                        "Cache-Control", "no-store", "Pragma", "no-cache");
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "invalid_client");
        body.put("error_description", "Client authentication failed");
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 401)
                .set(ApiResponse::setHeaders, headers)
                .set(ApiResponse::setBody, body)
                .build();
    }

    @Decision(POST)
    public boolean doPost() {
        return true;
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(IntrospectionRequest introspectionRequest,
                                     OidcApplication oidcApplication, DSLContext dsl) {
        String token = introspectionRequest.token();

        Map<String, Object> unverifiedPayload = RsaJwtSigner.extractUnverifiedClaims(token);
        if (unverifiedPayload == null) return inactiveResponse();

        Object clientIdObj = unverifiedPayload.get("client_id");
        if (!(clientIdObj instanceof String tokenClientId)) return inactiveResponse();
        if (!tokenClientId.equals(oidcApplication.clientId())) return inactiveResponse();

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        byte[] publicKey = repo.findPublicKeyByClientId(tokenClientId).orElse(null);
        if (publicKey == null) return inactiveResponse();

        Map<String, Object> claims = RsaJwtSigner.verify(token, publicKey);
        if (claims == null) return inactiveResponse();

        long now = config.getClock().instant().getEpochSecond();
        Object expObj = claims.get("exp");
        if (!(expObj instanceof Number) || ((Number) expObj).longValue() < now) {
            return inactiveResponse();
        }

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

        return jsonResponse(200, response);
    }

    private ApiResponse inactiveResponse() {
        return jsonResponse(200, Map.of("active", false));
    }

    private ApiResponse jsonResponse(int status, Map<String, ?> body) {
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, status)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, body)
                .build();
    }
}
