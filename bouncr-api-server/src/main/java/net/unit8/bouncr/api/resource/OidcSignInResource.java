package net.unit8.bouncr.api.resource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.exception.FalteringEnvironmentException;
import enkan.util.CodecUtils;
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
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private static final OkHttpClient OKHTTP = new OkHttpClient().newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private static final JwksVerifier JWKS_VERIFIER = new JwksVerifier(OKHTTP);

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
        if (oidcProvider != null && (oidcProvider.responseType() == ID_TOKEN || oidcProvider.responseType() == TOKEN)) {
            // Verify State
            if (oidcSession != null && !Objects.equals(params.get("state"), oidcSession.state())) {
                context.setMessage(Problem.valueOf(401, "State doesn't match", BouncrProblem.MISMATCH_STATE.problemUri()));
                return false;
            }
        }

        String redirectUriBase = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/bouncr/api";
        String redirectUri = oidcProvider != null && oidcProvider.redirectUri() != null
                ? oidcProvider.redirectUri().toString()
                : redirectUriBase + "/sign_in/oidc/" + (oidcProvider != null ? oidcProvider.name() : params.get("name"));

        HashMap<String, Object> res = Failsafe.with(config.getHttpClientRetryPolicy()).get(() -> {
            FormBody.Builder bodyBuilder = new FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", params.get("code"))
                    .add("redirect_uri", redirectUri);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(oidcProvider.tokenEndpoint());

            if (oidcProvider.tokenEndpointAuthMethod() == CLIENT_SECRET_POST) {
                bodyBuilder.add("client_id", oidcProvider.clientId())
                        .add("client_secret", oidcProvider.clientSecret());
            } else {
                requestBuilder.header("Authorization", "Basic " +
                        Base64.getUrlEncoder().encodeToString(
                                (oidcProvider.clientId() + ":" + oidcProvider.clientSecret()).getBytes()));
            }

            if (oidcProvider.pkceEnabled() && oidcSession != null && oidcSession.codeVerifier() != null) {
                bodyBuilder.add("code_verifier", oidcSession.codeVerifier());
            }

            Response response = OKHTTP.newCall(requestBuilder.post(bodyBuilder.build()).build()).execute();
            if (response.code() == 503) throw new FalteringEnvironmentException();

            try (InputStream in = response.body().byteStream()) {
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

        // Verify Nonce
        if (oidcSession != null && !Objects.equals(claim.getNonce(), oidcSession.nonce())) {
            context.setMessage(Problem.valueOf(401, "Nonce doesn't match", BouncrProblem.MISMATCH_NONCE.problemUri()));
            return false;
        }

        // Verify iss claim
        if (oidcProvider.issuer() != null && !oidcProvider.issuer().equals(claim.getIss())) {
            context.setMessage(Problem.valueOf(401, "ID token issuer doesn't match", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        // Verify aud claim
        if (claim.getAud() != null && !oidcProvider.clientId().equals(claim.getAud())) {
            context.setMessage(Problem.valueOf(401, "ID token audience doesn't match", BouncrProblem.INVALID_ID_TOKEN_CLAIMS.problemUri()));
            return false;
        }

        // Verify exp claim
        if (claim.getExp() != null && claim.getExp() < System.currentTimeMillis() / 1000) {
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

        SignInService signInService = new SignInService(dsl, storeProvider, config);
        String token = signInService.createToken();
        UserSession userSession = signInService.createUserSession(request, user, token);
        context.put(SESSION, userSession);
        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_IN, context);

        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        return Optional.ofNullable(config.getOidcConfiguration().getSignInRedirectUrl())
                .map(uri -> uriInterpolator.interpolate(uri, "token", userSession.token()))
                .map(uri -> uriInterpolator.interpolate(uri, "account", user.account()))
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uri.toString()
                        ))
                        .build())
                .orElse(builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 200)
                        .set(ApiResponse::setBody, userSession)
                        .build());
    }
}
