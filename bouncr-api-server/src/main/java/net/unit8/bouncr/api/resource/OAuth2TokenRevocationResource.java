package net.unit8.bouncr.api.resource;

import enkan.web.collection.Headers;
import enkan.collection.Parameters;
import enkan.web.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders;
import net.unit8.bouncr.data.RevocationRequest;
import net.unit8.bouncr.api.service.OAuth2ClientAuthenticator;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2RefreshToken;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OAUTH2_REFRESH_TOKEN;

/**
 * OAuth2 Token Revocation endpoint (RFC 7009).
 * {@code POST /oauth2/token/revoke} ({@code application/x-www-form-urlencoded})
 *
 * <p>Always returns 200 OK regardless of whether the token was valid.
 * Revokes authorization codes and refresh tokens with client ownership check.
 * JWT access tokens are self-contained and remain valid until expiry.
 */
@AllowedMethods("POST")
public class OAuth2TokenRevocationResource {
    static final ContextKey<RevocationRequest> REVOKE_REQ =
            ContextKey.of("revokeReq", RevocationRequest.class);
    static final ContextKey<OidcApplication> CLIENT_APP = ContextKey.of(OidcApplication.class);
    static final ContextKey<Boolean> BASIC_AUTH_ATTEMPTED = ContextKey.of("basicAuthAttempted", Boolean.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem isMalformed(Parameters params, RestContext context) {
        return switch (BouncrFormDecoders.REVOCATION_REQUEST.decode(params)) {
            case Ok<RevocationRequest> ok -> {
                context.put(REVOKE_REQ, ok.value());
                yield null;
            }
            case Err<RevocationRequest> err -> {
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
    public ApiResponse handleCreated(RevocationRequest revocationRequest,
                                     OidcApplication oidcApplication) {
        String token = revocationRequest.token();
        String authenticatedClientId = oidcApplication.credentials().clientId();

        // Revoke with ownership check (RFC 7009 §2.1)
        var authCodeData = storeProvider.getStore(AUTHORIZATION_CODE).read(token);
        if (authCodeData instanceof AuthorizationCode ac && authenticatedClientId.equals(ac.clientId())) {
            storeProvider.getStore(AUTHORIZATION_CODE).delete(token);
        }
        var refreshData = storeProvider.getStore(OAUTH2_REFRESH_TOKEN).read(token);
        if (refreshData instanceof OAuth2RefreshToken rt && authenticatedClientId.equals(rt.clientId())) {
            storeProvider.getStore(OAUTH2_REFRESH_TOKEN).delete(token);
        }
        // Revoke opaque access token
        var tokenData = storeProvider.getStore(BOUNCR_TOKEN).read(token);
        if (tokenData instanceof Map<?, ?> profileMap
                && authenticatedClientId.equals(profileMap.get("client_id"))) {
            storeProvider.getStore(BOUNCR_TOKEN).delete(token);
        }

        // Always 200 OK (RFC 7009 §2.2)
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, Map.of())
                .build();
    }
}
