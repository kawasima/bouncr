package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import enkan.util.CodecUtils;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OidcSession;
import net.unit8.bouncr.entity.OidcProvider;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;

/**
 * Initiates an OpenID Connect Authorization Code Flow by redirecting to the provider.
 */
@AllowedMethods({"GET"})
public class OidcAuthorizationResource {

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcProvider> query = cb.createQuery(OidcProvider.class);
        Root<OidcProvider> root = query.from(OidcProvider.class);
        query.where(cb.equal(root.get("name"), params.get("name")));
        OidcProvider oidcProvider = em.createQuery(query).getResultStream().findAny().orElse(null);
        if (oidcProvider != null) {
            context.putValue(oidcProvider);
        }
        return oidcProvider != null;
    }

    @Decision(HANDLE_OK)
    public ApiResponse authorize(OidcProvider oidcProvider, HttpRequest request, RestContext context) {
        OidcSession oidcSession = OidcSession.create(config.getSecureRandom());

        String redirectUriBase = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + "/bouncr/api";
        String redirectUri = Optional.ofNullable(oidcProvider.getRedirectUri())
                .orElse(redirectUriBase + "/sign_in/oidc/" + oidcProvider.getName());

        StringBuilder authorizationUrl = new StringBuilder(oidcProvider.getAuthorizationEndpoint())
                .append("?response_type=").append(CodecUtils.urlEncode(oidcProvider.getResponseType().getName()))
                .append("&client_id=").append(CodecUtils.urlEncode(oidcProvider.getClientId()))
                .append("&redirect_uri=").append(CodecUtils.urlEncode(redirectUri))
                .append("&state=").append(oidcSession.getState())
                .append("&scope=").append(CodecUtils.urlEncode(oidcProvider.getScope()))
                .append("&nonce=").append(oidcSession.getNonce());

        if (oidcProvider.isPkceEnabled()) {
            byte[] verifierBytes = new byte[32];
            config.getSecureRandom().nextBytes(verifierBytes);
            String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
            oidcSession.setCodeVerifier(codeVerifier);
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes("UTF-8"));
                String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
                authorizationUrl.append("&code_challenge=").append(codeChallenge)
                        .append("&code_challenge_method=S256");
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate PKCE code challenge", e);
            }
        }

        String oidcSessionId = storeProvider.getStore(OIDC_SESSION).write(null, oidcSession);

        String cookieHeader = "OIDC_SESSION_ID=" + oidcSessionId + "; HttpOnly; Path=/";

        Headers headers = Headers.of(
                "Location", authorizationUrl.toString(),
                "Set-Cookie", cookieHeader);

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 302)
                .set(ApiResponse::setHeaders, headers)
                .build();
    }

    @Decision(HANDLE_NOT_FOUND)
    public Problem handleNotFound() {
        return Problem.valueOf(404, "OIDC provider not found");
    }
}
