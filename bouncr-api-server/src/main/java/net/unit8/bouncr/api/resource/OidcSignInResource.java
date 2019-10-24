package net.unit8.bouncr.api.resource;

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
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;
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

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
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
        return oidcProvider != null;
    }

    @Decision(PROCESSABLE)
    public boolean processable(Parameters params, OidcSession oidcSession) {
        // The verification of state should be executed at frontend?
        /*
        if (!Objects.equals(oidcSession.getState(), params.get("state"))) {
            return false;
        }
        return true;
        */
        return true;
    }

    @Decision(HANDLE_OK)
    public ApiResponse callback(HttpRequest request,
                                Parameters params,
                                OidcProvider oidcProvider,
                                EntityManager em) {
        if (oidcProvider.getResponseType() == ID_TOKEN || oidcProvider.getResponseType() == TOKEN) {
            String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
            OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
            // Verify State
            if (oidcSession != null && !Objects.equals(params.get("state"), oidcSession.getState())) {
                return builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 401)
                        .set(ApiResponse::setBody, Problem.valueOf(401, "State doesn't match"))
                        .build();
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
            return builder(new ApiResponse())
                    .set(ApiResponse::setStatus, 401)
                    .set(ApiResponse::setBody, Problem.valueOf(401, "Can't authenticate by OpenID Connect"))
                    .build();
        }
        return connectOpenIdToBouncrUser(encodedIdToken, oidcProvider, request, em);
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
    private ApiResponse connectOpenIdToBouncrUser(String idToken, OidcProvider oidcProvider, HttpRequest request, EntityManager em) {
        String[] tokens = idToken.split("\\.", 3);
        JwtClaim claim = jsonWebToken.decodePayload(tokens[1], new TypeReference<>() {
        });

        // Verify Nonce
        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
        if (oidcSession != null && !Objects.equals(claim.getNonce(), oidcSession.getNonce())) {
            return builder(new ApiResponse())
                    .set(ApiResponse::setStatus, 401)
                    .set(ApiResponse::setBody, Problem.valueOf(401, "Nonce doesn't match"))
                    .build();
        }

        UriInterpolator uriInterpolator = config.getOidcConfiguration().getUriInterpolator();
        SignInService signInService = new SignInService(em, storeProvider, config);
        if (claim.getSub() != null) {
            OidcUser oidcUser = findOidcUser(oidcProvider, claim.getSub(), em);
            if (oidcUser != null) {
                String token = signInService.createToken();
                UserSession userSession = signInService.createUserSession(request, oidcUser.getUser(), token);
                return Optional.ofNullable(config.getOidcConfiguration().getSignInRedirectUrl())
                        .map(uri -> uriInterpolator.interpolate(uri, "token", userSession.getToken()))
                        .map(uri -> uriInterpolator.interpolate(uri, "account", oidcUser.getUser().getAccount()))
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
            } else {
                Invitation invitation = builder(new Invitation())
                        .set(Invitation::setCode, RandomUtils.generateRandomString(8, config.getSecureRandom()))
                        .set(Invitation::setInvitedAt, LocalDateTime.now())
                        .build();
                OidcInvitation oidcInvitation = builder(new OidcInvitation())
                        .set(OidcInvitation::setInvitation, invitation)
                        .set(OidcInvitation::setOidcProvider, oidcProvider)
                        .set(OidcInvitation::setOidcPayload, tokens[1])
                        .build();
                EntityTransactionManager tx = new EntityTransactionManager(em);
                tx.required(() -> {
                    em.persist(invitation);
                    em.persist(oidcInvitation);
                });

                if (Objects.equals(request.getHeaders().get("X-Requested-With"), "XMLHttpRequest")) {
                    return null; // Implicit
                } else {
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
            }
        }
        return Optional.ofNullable(config.getOidcConfiguration().getUnauthenticateRedirectUrl())
                .map(uri -> builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 302)
                        .set(ApiResponse::setHeaders, Headers.of(
                                "Location", uri.toString()
                        ))
                        .build())
                .orElse(builder(new ApiResponse())
                        .set(ApiResponse::setStatus, 401)
                        .set(ApiResponse::setBody, Problem.valueOf(401))
                        .build());
    }

    public static class OidcProviderDto implements Serializable {
        private Long id;
        private String name;
        private String clientId;
        private String scope;
        private String state = RandomUtils.generateRandomString(8);
        private String responseType;
        private String tokenEndpoint;
        private String authorizationEndpoint;
        private String nonce = RandomUtils.generateRandomString(32);
        private String redirectUriBase;

        public String getRedirectUri() {
            return redirectUriBase
                    + "/sign_in/oidc/" + name;
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
