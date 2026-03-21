package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.CodecUtils;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.PkceChallenge;
import net.unit8.bouncr.data.Scope;
import net.unit8.bouncr.data.UserIdentity;
import net.unit8.bouncr.util.RandomUtils;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;

/**
 * OAuth2 Authorization endpoint (Bouncr as OIDC IdP).
 * GET /oauth2/authorize
 */
@AllowedMethods("GET")
public class OAuth2AuthorizeResource {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(HANDLE_OK)
    public ApiResponse authorize(Parameters params,
                                 UserPermissionPrincipal principal,
                                 DSLContext dsl) {
        String responseType = params.get("response_type");
        String clientId = params.get("client_id");
        String redirectUri = params.get("redirect_uri");
        String scope = params.get("scope");
        String state = params.get("state");

        if (!"code".equals(responseType)) {
            return oauthError(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE,
                    "Only response_type=code is supported");
        }
        if (clientId == null || clientId.isBlank()) {
            return oauthError(OAuth2Error.INVALID_REQUEST, "client_id is required");
        }

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByClientId(clientId).orElse(null);
        if (app == null) {
            return oauthError(OAuth2Error.INVALID_CLIENT, "Unknown client_id");
        }

        // Compare as strings — URL.toString() may normalize ports, so this is an exact-match check.
        // Clients must send the exact same redirect_uri as registered.
        if (app.callbackUrl() == null) {
            return oauthError(OAuth2Error.INVALID_REQUEST, "Client has no registered callback URL");
        }
        String registeredCallback = app.callbackUrl().toString();
        if (redirectUri == null || !Objects.equals(redirectUri, registeredCallback)) {
            return oauthError(OAuth2Error.INVALID_REQUEST,
                    "redirect_uri does not match registered callback");
        }
        // Only allow https (or http for localhost/127.0.0.1 in development)
        try {
            URI parsed = URI.create(redirectUri);
            String host = parsed.getHost();
            boolean isLocalDev = "http".equals(parsed.getScheme())
                    && ("localhost".equals(host) || "127.0.0.1".equals(host));
            if (!"https".equals(parsed.getScheme()) && !isLocalDev) {
                return oauthError(OAuth2Error.INVALID_REQUEST,
                        "redirect_uri must use https (http allowed only for localhost)");
            }
        } catch (IllegalArgumentException e) {
            return oauthError(OAuth2Error.INVALID_REQUEST, "redirect_uri is not a valid URI");
        }

        // Validate scope — exact token match, not substring
        Set<String> scopes = scope != null ? new HashSet<>(Arrays.asList(scope.split("\\s+"))) : Set.of();
        if (!scopes.contains("openid")) {
            return redirectError(redirectUri, "invalid_scope", "scope must include openid", state);
        }

        // PKCE validation — reject code_challenge_method without code_challenge
        String codeChallenge = params.get("code_challenge");
        String codeChallengeMethod = params.get("code_challenge_method");
        if (codeChallengeMethod != null && codeChallenge == null) {
            return redirectError(redirectUri, "invalid_request",
                    "code_challenge is required when code_challenge_method is specified", state);
        }
        if (codeChallenge != null && !"S256".equals(codeChallengeMethod)) {
            return redirectError(redirectUri, "invalid_request",
                    "Only code_challenge_method=S256 is supported", state);
        }

        // Check if user is authenticated
        if (principal == null) {
            String currentUrl = "/oauth2/authorize?"
                    + "response_type=" + CodecUtils.urlEncode(responseType)
                    + "&client_id=" + CodecUtils.urlEncode(clientId)
                    + "&redirect_uri=" + CodecUtils.urlEncode(redirectUri)
                    + "&scope=" + CodecUtils.urlEncode(scope != null ? scope : "")
                    + (state != null ? "&state=" + CodecUtils.urlEncode(state) : "")
                    + (params.get("nonce") != null ? "&nonce=" + CodecUtils.urlEncode(params.get("nonce")) : "")
                    + (codeChallenge != null ? "&code_challenge=" + CodecUtils.urlEncode(codeChallenge)
                            + "&code_challenge_method=S256" : "");
            // Redirect to sign-in page; after authentication, user returns to this URL
            URI unauthRedirectUrl = config.getOidcConfiguration().getUnauthenticateRedirectUrl();
            if (unauthRedirectUrl != null) {
                // Use the configured unauthenticated redirect with return_url interpolation
                String target = config.getOidcConfiguration().getUriInterpolator()
                        .interpolate(unauthRedirectUrl, "return_url", currentUrl).toString();
                return redirect(target);
            }
            // Fallback: redirect to issuer base + sign-in path
            return redirect(config.getIssuerBaseUrl() + "/sign_in?return_url="
                    + CodecUtils.urlEncode(currentUrl));
        }

        // Generate authorization code
        long now = config.getClock().instant().getEpochSecond();
        String code = RandomUtils.generateRandomString(32, config.getSecureRandom());
        UserIdentity user = new UserIdentity(principal.getId(), principal.getName());
        PkceChallenge pkce = codeChallenge != null
                ? new PkceChallenge(codeChallenge, "S256") : null;
        AuthorizationCode authCode = new AuthorizationCode(
                clientId, user, Scope.parse(scope),
                params.get("nonce"), pkce, redirectUri, now);
        storeProvider.getStore(AUTHORIZATION_CODE).write(code, authCode);

        // Redirect to callback — preserve existing query params
        String separator = redirectUri.contains("?") ? "&" : "?";
        StringBuilder callbackUrl = new StringBuilder(redirectUri)
                .append(separator).append("code=").append(CodecUtils.urlEncode(code));
        if (state != null) {
            callbackUrl.append("&state=").append(CodecUtils.urlEncode(state));
        }
        return redirect(callbackUrl.toString());
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
