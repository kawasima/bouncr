package net.unit8.bouncr.api.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.exception.FalteringEnvironmentException;
import enkan.util.CodecUtils;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.jodah.failsafe.Failsafe;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.OidcSession;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.util.UriInterpolator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;
import static net.unit8.bouncr.entity.ActionType.USER_SIGNIN;
import static net.unit8.bouncr.entity.ResponseType.ID_TOKEN;
import static net.unit8.bouncr.entity.ResponseType.TOKEN;

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

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BeansConverter converter;

    @Inject
    private BouncrConfiguration config;

    @Inject
    private JsonWebToken jsonWebToken;

    private ObjectMapper jsonMapper = new ObjectMapper();

    @Decision(AUTHORIZED)
    public boolean authenticate(HttpRequest request,
                              Parameters params,
                              RestContext context,
                              EntityManager em) {
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_IN, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return false;
        }
        OidcProvider oidcProvider = findOidcProvider(params, context, em);
        if (oidcProvider.getResponseType() == ID_TOKEN || oidcProvider.getResponseType() == TOKEN) {
            String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
            OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
            // Verify State
            if (oidcSession != null && !Objects.equals(params.get("state"), oidcSession.getState())) {
                context.setMessage(builder(Problem.valueOf(401, "State doesn't match"))
                        .set(Problem::setType, BouncrProblem.MISMATCH_STATE.problemUri())
                        .build());
                return false;
            }
        }
        OidcProviderDto oidcProviderDto = converter.createFrom(oidcProvider, OidcProviderDto.class);
        oidcProviderDto.setRedirectUriBase(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/bouncr/api");

        HashMap<String, Object> res = Failsafe.with(config.getHttpClientRetryPolicy()).get(() -> {
            FormBody body = new FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", params.get("code"))
                    .add("redirect_uri", oidcProviderDto.getRedirectUri())
                    .build();
            Response response =  OKHTTP.newCall(new Request.Builder()
                    .url(oidcProvider.getTokenEndpoint())
                    .header("Authorization", "Basic " +
                            Base64.getUrlEncoder().encodeToString((oidcProvider.getClientId() + ":" + oidcProvider.getClientSecret()).getBytes()))
                    .post(body)
                    .build())
                    .execute();
            if (response.code() == 503) throw new FalteringEnvironmentException();

            try(InputStream in  = response.body().byteStream()) {
                return jsonMapper.readValue(in, GENERAL_JSON_REF);
            }
        });
        String encodedIdToken = (String) res.get("id_token");
        if (encodedIdToken == null) {
            context.setMessage(builder(Problem.valueOf(401, Objects.toString(res.get("error"), "Can't authenticate by OpenID Connect")))
                    .set(Problem::setType, BouncrProblem.OPENID_PROVIDER_RETURNS_ERROR.problemUri())
                    .build());
            return false;
        }
        String[] tokens = encodedIdToken.split("\\.", 3);
        JwtClaim claim = jsonWebToken.decodePayload(tokens[1], new TypeReference<>() {
        });

        // Verify Nonce
        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
        if (oidcSession != null && !Objects.equals(claim.getNonce(), oidcSession.getNonce())) {
            context.setMessage(builder(Problem.valueOf(401, Objects.toString(res.get("error"), "Can't authenticate by OpenID Connect")))
                    .set(Problem::setType, BouncrProblem.MISMATCH_NONCE.problemUri())
                    .build());
        }

        if (claim.getSub() == null) {
            context.setMessage(builder(Problem.valueOf(401, Objects.toString(res.get("error"), "Can't authenticate by OpenID Connect")))
                    .set(Problem::setType, BouncrProblem.MISSING_SUBJECT.problemUri())
                    .build());
        }
        OidcUser oidcUser = findOidcUser(oidcProvider, claim.getSub(), em);
        if (oidcUser == null) {
            createInvitation(tokens[1], oidcProvider, request, context, em);
            return false;
        }

        if (oidcUser.getUser().getUserLock() != null) {
            context.setMessage(builder(Problem.valueOf(401, "Account is locked"))
                    .set(Problem::setType, BouncrProblem.ACCOUNT_IS_LOCKED.problemUri())
                    .build());
        }
        context.putValue(oidcUser.getUser());
        return true;
    }

    private void createInvitation(String payload,
                                         OidcProvider oidcProvider,
                                         HttpRequest request,
                                         RestContext context,
                                         EntityManager em) {
        Invitation invitation = builder(new Invitation())
                .set(Invitation::setCode, RandomUtils.generateRandomString(8, config.getSecureRandom()))
                .set(Invitation::setInvitedAt, LocalDateTime.now())
                .build();
        OidcInvitation oidcInvitation = builder(new OidcInvitation())
                .set(OidcInvitation::setInvitation, invitation)
                .set(OidcInvitation::setOidcProvider, oidcProvider)
                .set(OidcInvitation::setOidcPayload, payload)
                .build();
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            em.persist(invitation);
            em.persist(oidcInvitation);
        });
        context.putValue(invitation);
    }

    private ApiResponse handleInvitation(Invitation invitation) {
        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        return Optional.ofNullable(config.getOidcConfiguration().getSignUpRedirectUrl())
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uriInterpolator.interpolate(uri, "code", invitation.getCode()).toString()
                        ))
                        .build())
                .orElse(
                        builder(new ApiResponse())
                                .set(ApiResponse::setStatus, 202)
                                .set(ApiResponse::setBody, Map.of(
                                        "code", invitation.getCode(),
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


    private OidcProvider findOidcProvider(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcProvider> query = cb.createQuery(OidcProvider.class);
        Root<OidcProvider> root = query.from(OidcProvider.class);
        query.where(cb.equal(root.get("name"), params.get("name")));

        OidcProvider oidcProvider = em.createQuery(query)
                .getResultStream()
                .findAny()
                .orElse(null);
        if (oidcProvider != null) {
            context.putValue(oidcProvider);
        }
        return oidcProvider;
    }


    private OidcUser findOidcUser(OidcProvider oidcProvider, String sub, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OidcUser> query = cb.createQuery(OidcUser.class);
        Root<OidcUser> root = query.from(OidcUser.class);
        root.fetch("user");
        query.where(cb.equal(root.get("oidcSub"), sub),
                cb.equal(root.get("oidcProvider"), oidcProvider));
        return em.createQuery(query).getResultStream().findAny().orElse(null);
    }

    @Decision(HANDLE_OK)
    public ApiResponse signIn(User user,
                              HttpRequest request,
                              ActionRecord actionRecord,
                              RestContext context,
                              EntityManager em) {
        actionRecord.setActor(user.getAccount());
        actionRecord.setActionType(USER_SIGNIN);

        SignInService signInService = new SignInService(em, storeProvider, config);
        String token = signInService.createToken();
        UserSession userSession = signInService.createUserSession(request, user, token);
        context.putValue(userSession);
        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_IN, context);

        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        return Optional.ofNullable(config.getOidcConfiguration().getSignInRedirectUrl())
                .map(uri -> uriInterpolator.interpolate(uri, "token", userSession.getToken()))
                .map(uri -> uriInterpolator.interpolate(uri, "account", user.getAccount()))
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

    public static class OidcProviderDto implements Serializable {
        private Long id;
        private String name;
        @JsonProperty("client_id")
        private String clientId;
        private String scope;
        private String state = RandomUtils.generateRandomString(8);
        @JsonProperty("response_type")
        private String responseType;
        @JsonProperty("token_endpoint")
        private String tokenEndpoint;
        @JsonProperty("authorization_endpoint")
        private String authorizationEndpoint;
        private String nonce = RandomUtils.generateRandomString(32);
        @JsonProperty("redirect_uri")
        private String redirectUri;
        private String redirectUriBase;

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getRedirectUri() {
            return Optional.ofNullable(redirectUri)
                    .orElse(redirectUriBase + "/sign_in/oidc/" + name);
        }

        public String getAuthorizationUrl() {
            return authorizationEndpoint + "?response_type=" + CodecUtils.urlEncode(responseType)
                    + "&client_id=" + clientId
                    + "&redirect_uri=" + CodecUtils.urlEncode(getRedirectUri())
                    + "&state=" + state
                    + "&scope=" + scope
                    + "&nonce=" + nonce;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getResponseType() {
            return responseType;
        }

        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public void setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
        }

        public String getAuthorizationEndpoint() {
            return authorizationEndpoint;
        }

        public void setAuthorizationEndpoint(String authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public String getRedirectUriBase() {
            return redirectUriBase;
        }

        public void setRedirectUriBase(String redirectUriBase) {
            this.redirectUriBase = redirectUriBase;
        }
    }
}
