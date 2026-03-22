package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.CodecUtils;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders;
import net.unit8.bouncr.api.decoder.BouncrFormDecoders.AuthorizeRequest;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.PkceChallenge;
import net.unit8.bouncr.data.UserIdentity;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;

/**
 * OAuth2 Authorization endpoint (Bouncr as OIDC IdP).
 * GET /oauth2/authorize
 */
@AllowedMethods("GET")
public class OAuth2AuthorizeResource {
    static final ContextKey<AuthorizeRequest> AUTHORIZE_REQ =
            ContextKey.of("authorizeRequest", AuthorizeRequest.class);
    static final ContextKey<OidcApplication> CLIENT_APP = ContextKey.of(OidcApplication.class);
    static final ContextKey<ApiResponse> NOT_FOUND_RESPONSE =
            ContextKey.of("notFoundResponse", ApiResponse.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "GET")
    public Problem isMalformed(Parameters params, RestContext context) {
        return switch (BouncrFormDecoders.AUTHORIZE_REQUEST.decode(params)) {
            case Ok<AuthorizeRequest> ok -> {
                context.put(AUTHORIZE_REQ, ok.value());
                yield null;
            }
            case Err<AuthorizeRequest> err -> {
                List<Problem.Violation> violations = err.issues().asList().stream()
                        .map(issue -> new Problem.Violation(
                                issue.path().toString(), issue.code(), issue.message()))
                        .toList();
                yield Problem.fromViolationList(violations);
            }
        };
    }

    @Decision(EXISTS)
    public boolean exists(AuthorizeRequest authorizeRequest, RestContext context, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByClientId(authorizeRequest.clientId()).orElse(null);
        if (app == null) {
            context.put(NOT_FOUND_RESPONSE, oauthError(OAuth2Error.INVALID_CLIENT, "Unknown client_id"));
            return false;
        }

        if (app.callbackUrl() == null) {
            context.put(NOT_FOUND_RESPONSE,
                    oauthError(OAuth2Error.INVALID_REQUEST, "Client has no registered callback URL"));
            return false;
        }

        String redirectUri = authorizeRequest.redirectUri();
        String registeredCallback = app.callbackUrl().toString();
        if (!Objects.equals(redirectUri, registeredCallback)) {
            context.put(NOT_FOUND_RESPONSE,
                    oauthError(OAuth2Error.INVALID_REQUEST,
                            "redirect_uri does not match registered callback"));
            return false;
        }

        try {
            URI parsed = URI.create(redirectUri);
            String host = parsed.getHost();
            boolean isLocalDev = "http".equals(parsed.getScheme())
                    && ("localhost".equals(host) || "127.0.0.1".equals(host));
            if (!"https".equals(parsed.getScheme()) && !isLocalDev) {
                context.put(NOT_FOUND_RESPONSE,
                        oauthError(OAuth2Error.INVALID_REQUEST,
                                "redirect_uri must use https (http allowed only for localhost)"));
                return false;
            }
        } catch (IllegalArgumentException e) {
            context.put(NOT_FOUND_RESPONSE,
                    oauthError(OAuth2Error.INVALID_REQUEST, "redirect_uri is not a valid URI"));
            return false;
        }

        context.put(CLIENT_APP, app);
        return true;
    }

    @Decision(HANDLE_NOT_FOUND)
    public ApiResponse handleNotFound(RestContext context) {
        return context.get(NOT_FOUND_RESPONSE)
                .orElseGet(() -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 404)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Content-Type", "application/json",
                                "Cache-Control", "no-store",
                                "Pragma", "no-cache"))
                        .set(ApiResponse::setBody, Map.of(
                                "error", "not_found",
                                "error_description", "Resource not found"))
                        .build());
    }

    @Decision(AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(HANDLE_UNAUTHORIZED)
    public ApiResponse handleUnauthorized(AuthorizeRequest authorizeRequest) {
        String currentUrl = toAuthorizeUrl(authorizeRequest);

        URI unauthRedirectUrl = config.getOidcConfiguration().getUnauthenticateRedirectUrl();
        if (unauthRedirectUrl != null) {
            String target = config.getOidcConfiguration().getUriInterpolator()
                    .interpolate(unauthRedirectUrl, "return_url", currentUrl).toString();
            return redirect(target);
        }

        return redirect(config.getIssuerBaseUrl() + "/sign_in?return_url="
                + CodecUtils.urlEncode(currentUrl));
    }

    @Decision(HANDLE_OK)
    public ApiResponse authorize(AuthorizeRequest authorizeRequest,
                                 Parameters params,
                                 UserPermissionPrincipal principal) {
        if (!"code".equals(authorizeRequest.responseType())) {
            return oauthError(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE,
                    "Only response_type=code is supported");
        }

        String redirectUri = authorizeRequest.redirectUri();
        String state = authorizeRequest.state();

        if (!authorizeRequest.scope().contains("openid")) {
            return redirectError(redirectUri, "invalid_scope", "scope must include openid", state);
        }

        String codeChallenge = params.get("code_challenge");
        String codeChallengeMethod = params.get("code_challenge_method");
        if (codeChallengeMethod != null && codeChallenge == null) {
            return redirectError(redirectUri, "invalid_request",
                    "code_challenge is required when code_challenge_method is specified", state);
        }
        if (codeChallenge != null && authorizeRequest.pkce() == null) {
            return redirectError(redirectUri, "invalid_request",
                    "Only code_challenge_method=S256 is supported", state);
        }

        long now = config.getClock().instant().getEpochSecond();
        String code = RandomUtils.generateRandomString(32, config.getSecureRandom());
        UserIdentity user = new UserIdentity(principal.getId(), principal.getName());
        PkceChallenge pkce = codeChallenge != null
                ? authorizeRequest.pkce()
                : null;

        AuthorizationCode authCode = new AuthorizationCode(
                authorizeRequest.clientId(),
                user,
                authorizeRequest.scope(),
                authorizeRequest.nonce(),
                pkce,
                redirectUri,
                now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        String separator = redirectUri.contains("?") ? "&" : "?";
        StringBuilder callbackUrl = new StringBuilder(redirectUri)
                .append(separator).append("code=").append(CodecUtils.urlEncode(code));
        if (state != null) {
            callbackUrl.append("&state=").append(CodecUtils.urlEncode(state));
        }
        return redirect(callbackUrl.toString());
    }

    private String toAuthorizeUrl(AuthorizeRequest authorizeRequest) {
        StringBuilder currentUrl = new StringBuilder("/oauth2/authorize?")
                .append("response_type=").append(CodecUtils.urlEncode(authorizeRequest.responseType()))
                .append("&client_id=").append(CodecUtils.urlEncode(authorizeRequest.clientId()))
                .append("&redirect_uri=").append(CodecUtils.urlEncode(authorizeRequest.redirectUri()))
                .append("&scope=").append(CodecUtils.urlEncode(authorizeRequest.scope().toString()));

        if (authorizeRequest.state() != null) {
            currentUrl.append("&state=").append(CodecUtils.urlEncode(authorizeRequest.state()));
        }
        if (authorizeRequest.nonce() != null) {
            currentUrl.append("&nonce=").append(CodecUtils.urlEncode(authorizeRequest.nonce()));
        }
        if (authorizeRequest.pkce() != null) {
            currentUrl.append("&code_challenge=").append(CodecUtils.urlEncode(authorizeRequest.pkce().challenge()))
                    .append("&code_challenge_method=").append(CodecUtils.urlEncode(authorizeRequest.pkce().method()));
        }
        return currentUrl.toString();
    }

    private ApiResponse redirect(String location) {
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 302)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Location", location,
                        "Cache-Control", "no-store"))
                .build();
    }

    private ApiResponse redirectError(String redirectUri, String error, String description, String state) {
        String separator = redirectUri.contains("?") ? "&" : "?";
        StringBuilder url = new StringBuilder(redirectUri)
                .append(separator).append("error=").append(CodecUtils.urlEncode(error));
        if (description != null) {
            url.append("&error_description=").append(CodecUtils.urlEncode(description));
        }
        if (state != null) {
            url.append("&state=").append(CodecUtils.urlEncode(state));
        }
        return redirect(url.toString());
    }

    private ApiResponse oauthError(OAuth2Error error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) {
            body.put("error_description", description);
        }
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store",
                        "Pragma", "no-cache"))
                .set(ApiResponse::setBody, body)
                .build();
    }
}
