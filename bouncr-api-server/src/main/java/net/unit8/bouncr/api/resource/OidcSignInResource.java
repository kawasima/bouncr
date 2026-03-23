package net.unit8.bouncr.api.resource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import net.unit8.bouncr.api.util.BouncrCookies;
import enkan.exception.FalteringEnvironmentException;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import dev.failsafe.Failsafe;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.InvitationRepository;
import net.unit8.bouncr.api.repository.OidcProviderRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.service.JwksVerifier;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.*;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.util.UriInterpolator;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import tools.jackson.core.JacksonException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;
import static net.unit8.bouncr.data.ActionType.USER_SIGNIN;
import static net.unit8.bouncr.data.ResponseType.ID_TOKEN;
import static net.unit8.bouncr.data.ResponseType.TOKEN;
import static net.unit8.bouncr.data.TokenEndpointAuthMethod.CLIENT_SECRET_POST;

/**
 * A Callback Endpoint from an OpenID Connect provider.
 *
 * @author kawasima
 */
@AllowedMethods({"GET"})
public class OidcSignInResource {
    private static final TypeReference<HashMap<String, Object>> GENERAL_JSON_REF = new TypeReference<>() {
    };

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final JwksVerifier JWKS_VERIFIER = new JwksVerifier(HTTP_CLIENT);

    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<Invitation> INVITATION = ContextKey.of(Invitation.class);
    static final ContextKey<OidcProvider> OIDC_PROVIDER = ContextKey.of(OidcProvider.class);
    static final ContextKey<UserSession> SESSION = ContextKey.of(UserSession.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Inject
    private JsonWebToken jsonWebToken;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Decision(AUTHORIZED)
    public boolean authenticate(HttpRequest request,
                                Parameters params,
                                RestContext context,
                                DSLContext dsl) {
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_IN, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return false;
        }
        OidcProviderRepository providerRepo = new OidcProviderRepository(dsl);
        OidcProvider oidcProvider = providerRepo.findByName(params.get("name")).orElse(null);
        if (oidcProvider != null) {
            context.put(OIDC_PROVIDER, oidcProvider);
        }

        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
        // OIDC session must exist to validate state and nonce (RFC 6749 §10.12)
        if (oidcSession == null) {
            context.setMessage(Problem.valueOf(401, "OIDC session not found", BouncrProblem.OIDC_SESSION_NOT_FOUND.problemUri()));
            return false;
        }
        if (!Objects.equals(params.get("state"), oidcSession.state())) {
            context.setMessage(Problem.valueOf(401, "State doesn't match", BouncrProblem.MISMATCH_STATE.problemUri()));
            return false;
        }

        String redirectUriBase = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/bouncr/api";
        String redirectUri = oidcProvider != null && oidcProvider.redirectUri() != null
                ? oidcProvider.redirectUri().toString()
                : redirectUriBase + "/sign_in/oidc/" + (oidcProvider != null ? oidcProvider.name() : params.get("name"));

        HashMap<String, Object> res = Failsafe.with(config.getHttpClientRetryPolicy()).get(() -> {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("grant_type", "authorization_code");
            form.put("code", params.get("code"));
            form.put("redirect_uri", redirectUri);

            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(oidcProvider.tokenEndpoint()))
                    .timeout(Duration.ofSeconds(10))
                    .header("content-type", "application/x-www-form-urlencoded");

            if (oidcProvider.tokenEndpointAuthMethod() == CLIENT_SECRET_POST) {
                form.put("client_id", oidcProvider.clientId());
                form.put("client_secret", oidcProvider.clientSecret());
            } else {
                requestBuilder.header("Authorization", "Basic " +
                        Base64.getEncoder().encodeToString(
                                (oidcProvider.clientId() + ":" + oidcProvider.clientSecret()).getBytes(StandardCharsets.UTF_8)));
            }

            if (oidcProvider.pkceEnabled() && oidcSession.codeVerifier() != null) {
                form.put("code_verifier", oidcSession.codeVerifier());
            }

            java.net.http.HttpRequest requestToTokenEndpoint = requestBuilder
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(toFormBody(form)))
                    .build();
            HttpResponse<InputStream> response;
            try {
                response = HTTP_CLIENT.send(requestToTokenEndpoint, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while calling OIDC token endpoint", e);
            }
            if (response.statusCode() == 503) throw new FalteringEnvironmentException();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream errorStream = response.body()) {
                    if (errorStream != null) {
                        try {
                            return jsonMapper.readValue(errorStream, GENERAL_JSON_REF);
                        } catch (JacksonException ignored) {
                            // Fall through to generic error
                        }
                    }
                }
                HashMap<String, Object> errorRes = new HashMap<>();
                errorRes.put("error", "Token endpoint returned HTTP " + response.statusCode());
                return errorRes;
            }

            try (InputStream in = response.body()) {
                if (in == null) {
                    HashMap<String, Object> errorRes = new HashMap<>();
                    errorRes.put("error", "Token endpoint returned empty body");
                    return errorRes;
                }
                return jsonMapper.readValue(in, GENERAL_JSON_REF);
            }
        });
        String encodedIdToken = (String) res.get("id_token");
        if (encodedIdToken == null) {
            context.setMessage(Problem.valueOf(401, Objects.toString(res.get("error"), "Can't authenticate by OpenID Connect"), BouncrProblem.OPENID_PROVIDER_RETURNS_ERROR.problemUri()));
            return false;
        }

        // Verify ID token signature
        if (!JWKS_VERIFIER.verify(encodedIdToken, oidcProvider)) {
            context.setMessage(Problem.valueOf(401, "ID token signature verification failed", BouncrProblem.INVALID_ID_TOKEN_SIGNATURE.problemUri()));
            return false;
        }

        String[] tokens = encodedIdToken.split("\\.", 3);
        JwtClaim claim = jsonWebToken.decodePayload(tokens[1], new TypeReference<>() {
        });

        // Verify Nonce (OpenID Connect Core §3.1.3.3)
        if (!Objects.equals(claim.getNonce(), oidcSession.nonce())) {
            context.setMessage(Problem.valueOf(401, "Nonce doesn't match", BouncrProblem.MISMATCH_NONCE.problemUri()));
            return false;
        }

        // Verify iss claim (OpenID Connect Core §3.1.3.3)
        if (oidcProvider.issuer() == null) {
            context.setMessage(Problem.valueOf(401, "OIDC provider issuer not configured", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }
        if (!oidcProvider.issuer().equals(claim.getIss())) {
            context.setMessage(Problem.valueOf(401, "ID token issuer doesn't match", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        // Verify aud claim (RFC 7519 §4.1.3 — aud can be string or array)
        if (claim.getAud() == null) {
            context.setMessage(Problem.valueOf(401, "ID token missing audience", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        boolean audValid;
        Object aud = claim.getAud();
        if (aud instanceof String audStr) {
            audValid = oidcProvider.clientId().equals(audStr);
        } else if (aud instanceof List<?> audList) {
            audValid = audList.contains(oidcProvider.clientId());
        } else {
            audValid = false;
        }
        if (!audValid) {
            context.setMessage(Problem.valueOf(401, "ID token audience doesn't match", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        // Verify azp claim (OpenID Connect Core §3.1.3.3)
        if (aud instanceof List<?> audList && audList.size() > 1) {
            // When aud is multi-valued, azp MUST be present and match client_id
            if (claim.getAzp() == null || !oidcProvider.clientId().equals(claim.getAzp())) {
                context.setMessage(Problem.valueOf(401, "ID token authorized party doesn't match", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
                return false;
            }
        }
        // If azp is present in any case, it must match client_id
        if (claim.getAzp() != null && !oidcProvider.clientId().equals(claim.getAzp())) {
            context.setMessage(Problem.valueOf(401, "ID token authorized party doesn't match", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        // Verify exp claim (allow 30 seconds clock skew)
        long clockSkewSeconds = 30;
        if (claim.getExp() != null && claim.getExp() + clockSkewSeconds < System.currentTimeMillis() / 1000) {
            context.setMessage(Problem.valueOf(401, "ID token has expired", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        if (claim.getSub() == null) {
            context.setMessage(Problem.valueOf(401, "ID token missing subject", BouncrProblem.MISSING_SUBJECT.problemUri()));
            return false;
        }

        OidcUser oidcUser = findOidcUser(dsl, oidcProvider, claim.getSub());
        if (oidcUser == null) {
            createInvitation(dsl, tokens[1], oidcProvider, request, context);
            return false;
        }

        // Check user lock by loading full user
        User user = oidcUser.user();
        if (user != null) {
            UserRepository userRepo = new UserRepository(dsl);
            if (userRepo.isLocked(user.id())) {
                context.setMessage(Problem.valueOf(401, "Account is locked", BouncrProblem.ACCOUNT_IS_LOCKED.problemUri()));
                return false;
            }
        }

        context.put(USER, user);
        return true;
    }

    private void createInvitation(DSLContext dsl,
                                  String payload,
                                  OidcProvider oidcProvider,
                                  HttpRequest request,
                                  RestContext context) {
        InvitationRepository invitationRepo = new InvitationRepository(dsl);
        String code = RandomUtils.generateRandomString(8, config.getSecureRandom());
        Invitation invitation = invitationRepo.insert(null, code, LocalDateTime.now(), null);
        invitationRepo.insertOidcInvitation(invitation.id(), oidcProvider.id(), payload);
        context.put(INVITATION, invitation);
    }

    private ApiResponse handleInvitation(Invitation invitation) {
        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        return Optional.ofNullable(config.getOidcConfiguration().getSignUpRedirectUrl())
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uriInterpolator.interpolate(uri, "code", invitation.code()).toString()
                        ))
                        .build())
                .orElse(
                        builder(new ApiResponse())
                                .set(ApiResponse::setStatus, 202)
                                .set(ApiResponse::setBody, Map.of(
                                        "code", invitation.code(),
                                        "message", ""
                                ))
                                .build()
                );
    }

    @Decision(HANDLE_UNAUTHORIZED)
    public ApiResponse handleUnauthorized(Invitation invitation, RestContext context) {
        if (invitation != null) {
            return handleInvitation(invitation);
        }

        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        URI problemType = context.getMessage()
                .filter(Problem.class::isInstance)
                .map(Problem.class::cast)
                .map(Problem::getType)
                .orElse(null);
        return Optional.ofNullable(config.getOidcConfiguration().getUnauthenticateRedirectUrl())
                .map(uri -> uriInterpolator.interpolate(uri, "problem", problemType.toString()))
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uri.toString()
                        ))
                        .build())
                .orElse(builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 401)
                        .set(ApiResponse::setBody, context.getMessage()
                                .orElse(Problem.valueOf(401)))
                        .build());
    }

    @Decision(ALLOWED)
    public boolean allowed(RestContext context) {
        config.getHookRepo().runHook(HookPoint.ALLOWED_SIGN_IN, context);
        return !context.getMessage().filter(Problem.class::isInstance).isPresent();
    }

    @Decision(HANDLE_FORBIDDEN)
    public ApiResponse handleForbidden(RestContext context) {
        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        URI problemType = context.getMessage()
                .filter(Problem.class::isInstance)
                .map(Problem.class::cast)
                .map(Problem::getType)
                .orElse(null);
        return Optional.ofNullable(config.getOidcConfiguration().getUnauthorizeRedirectUrl())
                .map(uri -> uriInterpolator.interpolate(uri, "problem", problemType.toString()))
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uri.toString()
                        ))
                        .build())
                .orElse(builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 403)
                        .set(ApiResponse::setBody, context.getMessage()
                                .orElse(Problem.valueOf(403)))
                        .build());
    }

    private OidcUser findOidcUser(DSLContext dsl, OidcProvider oidcProvider, String sub) {
        UserRepository userRepo = new UserRepository(dsl);
        return userRepo.findOidcUser(oidcProvider.id(), sub)
                .map(ou -> new OidcUser(oidcProvider, ou.user(), ou.oidcSub()))
                .orElse(null);
    }

    @Decision(HANDLE_OK)
    public ApiResponse signIn(User user,
                              HttpRequest request,
                              ActionRecord actionRecord,
                              RestContext context,
                              DSLContext dsl) {
        actionRecord.setActor(user.account());
        actionRecord.setActionType(USER_SIGNIN);

        // Clean up OIDC session to prevent replay
        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        if (oidcSessionId != null) {
            storeProvider.getStore(OIDC_SESSION).delete(oidcSessionId);
        }
        BouncrCookies cookies = new BouncrCookies(config);
        String clearCookie = cookies.clearSession("OIDC_SESSION_ID").toHttpString();

        SignInService signInService = new SignInService(dsl, storeProvider, config);
        String token = signInService.createToken();
        UserSession userSession = signInService.createUserSession(request, user, token);
        context.put(SESSION, userSession);
        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_IN, context);

        String tokenCookie = cookies.token(userSession.token()).toHttpString();

        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        return Optional.ofNullable(config.getOidcConfiguration().getSignInRedirectUrl())
                .map(uri -> uriInterpolator.interpolate(uri, "account", user.account()))
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uri.toString(),
                                "Set-Cookie", clearCookie,
                                "Set-Cookie", tokenCookie))
                        .build())
                .orElse(builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 200)
                        .set(ApiResponse::setHeaders, Headers.of("Set-Cookie", clearCookie, "Set-Cookie", tokenCookie))
                        .set(ApiResponse::setBody, userSession)
                        .build());
    }

    private static String toFormBody(Map<String, String> form) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(Objects.toString(entry.getValue(), ""), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
