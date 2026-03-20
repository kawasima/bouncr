package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import enkan.util.CodecUtils;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcProviderRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.bouncr.data.OidcSession;
import net.unit8.bouncr.util.RandomUtils;
import org.jooq.DSLContext;

import jakarta.inject.Inject;

import java.security.MessageDigest;
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
    static final ContextKey<OidcProvider> PROVIDER = ContextKey.of(OidcProvider.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        Optional<OidcProvider> provider = repo.findByName(params.get("name"));
        provider.ifPresent(p -> context.put(PROVIDER, p));
        return provider.isPresent();
    }

    @Decision(HANDLE_OK)
    public ApiResponse authorize(OidcProvider oidcProvider, HttpRequest request, RestContext context) {
        String nonce = RandomUtils.generateRandomString(16, config.getSecureRandom());
        String state = RandomUtils.generateRandomString(16, config.getSecureRandom());
        OidcSession oidcSession = new OidcSession(nonce, state, null, null);

        String redirectUriBase = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + "/bouncr/api";
        String redirectUri = Optional.ofNullable(oidcProvider.redirectUri())
                .map(Object::toString)
                .orElse(redirectUriBase + "/sign_in/oidc/" + oidcProvider.name());

        StringBuilder authorizationUrl = new StringBuilder(oidcProvider.authorizationEndpoint())
                .append("?response_type=").append(CodecUtils.urlEncode(oidcProvider.responseType().getName()))
                .append("&client_id=").append(CodecUtils.urlEncode(oidcProvider.clientId()))
                .append("&redirect_uri=").append(CodecUtils.urlEncode(redirectUri))
                .append("&state=").append(oidcSession.state())
                .append("&scope=").append(CodecUtils.urlEncode(oidcProvider.scope()))
                .append("&nonce=").append(oidcSession.nonce());

        if (oidcProvider.pkceEnabled()) {
            byte[] verifierBytes = new byte[32];
            config.getSecureRandom().nextBytes(verifierBytes);
            String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
            oidcSession = new OidcSession(oidcSession.nonce(), oidcSession.state(),
                    oidcSession.responseType(), codeVerifier);
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

        String cookieHeader = "OIDC_SESSION_ID=" + oidcSessionId + "; HttpOnly; Secure; SameSite=Lax; Path=/";

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
