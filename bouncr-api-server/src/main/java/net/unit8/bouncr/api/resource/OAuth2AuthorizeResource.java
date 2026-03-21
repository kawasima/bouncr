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
import net.unit8.bouncr.util.RandomUtils;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
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

        if (redirectUri == null || !Objects.equals(redirectUri, app.callbackUrl().toString())) {
            return oauthError(OAuth2Error.INVALID_REQUEST,
                    "redirect_uri does not match registered callback");
        }

        // Validate scope — exact token match, not substring
        Set<String> scopes = scope != null ? Set.of(scope.split("\\s+")) : Set.of();
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
                    + "&scope=" + CodecUtils.urlEncode(scope)
                    + (state != null ? "&state=" + CodecUtils.urlEncode(state) : "")
                    + (params.get("nonce") != null ? "&nonce=" + CodecUtils.urlEncode(params.get("nonce")) : "")
                    + (codeChallenge != null ? "&code_challenge=" + CodecUtils.urlEncode(codeChallenge)
                            + "&code_challenge_method=S256" : "");
            // Redirect to sign-in page; after authentication, user returns to this URL
            String signInUrl = config.getOidcConfiguration().getSignInUrl();
            if (signInUrl == null) {
                signInUrl = config.getIssuerBaseUrl() + "/bouncr/api/sign_in";
            }
            return redirect(signInUrl + "?return_url=" + CodecUtils.urlEncode(currentUrl));
        }

        // Generate authorization code
        long now = config.getClock().instant().getEpochSecond();
        String code = RandomUtils.generateRandomString(32, config.getSecureRandom());
        AuthorizationCode authCode = new AuthorizationCode(
                clientId, principal.getId(), principal.getName(),
                scope, params.get("nonce"), codeChallenge, redirectUri, now);
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
                .set(ApiResponse::setHeaders, Headers.of("Location", location))
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
        java.util.Map<String, String> body = new java.util.LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) {
            body.put("error_description", description);
        }
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, Headers.of("Content-Type", "application/json"))
                .set(ApiResponse::setBody, body)
                .build();
    }
}
