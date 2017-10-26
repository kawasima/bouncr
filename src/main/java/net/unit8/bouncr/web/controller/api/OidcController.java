package net.unit8.bouncr.web.controller.api;

import enkan.collection.Parameters;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.session.KeyValueStore;
import enkan.util.CodecUtils;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.sign.JwtHeader;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.controller.data.AccessToken;
import net.unit8.bouncr.web.dao.OidcApplicationDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.OidcApplication;
import net.unit8.bouncr.web.entity.ResponseType;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.service.OAuthService;
import net.unit8.bouncr.web.service.SignInService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.FOUND;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.HttpResponseUtils.contentType;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.ACCESS_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;
import static net.unit8.bouncr.web.entity.OAuth2Error.*;
import static net.unit8.bouncr.web.entity.ResponseType.*;

public class OidcController {

    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private JsonWebToken jsonWebToken;

    @Inject
    private BouncrConfiguration config;

    private SignInService signInService;
    private OAuthService oauthService;

    @PostConstruct
    private void initialize() {
        signInService = new SignInService(daoProvider, storeProvider, config);
        oauthService = new OAuthService();
    }

    private String createIdToken(Long userId, OidcApplication oidcApplication, String nonce) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(userId);
        return jsonWebToken.sign(builder(new JwtClaim())
                        .set(JwtClaim::setSub, user.getId().toString())
                        .set(JwtClaim::setIss, "")
                        .set(JwtClaim::setAud, oidcApplication.getClientId())
                        .set(JwtClaim::setEmail, user.getEmail())
                        .set(JwtClaim::setName, user.getName())
                        .set(JwtClaim::setPreferredUsername, user.getAccount())
                        .set(JwtClaim::setIat, (System.currentTimeMillis() / 1000) - 60)
                        .set(JwtClaim::setExp, (System.currentTimeMillis() / 1000) + 3600)
                        .set(JwtClaim::setNonce, nonce)
                        .build(),
                builder(new JwtHeader())
                        .set(JwtHeader::setAlg, "RS256")
                        .set(JwtHeader::setKid, "")
                        .build(),
                KeyUtils.decode(oidcApplication.getPrivateKey()));
    }

    private String createAccessToken(String account, String clientId) {
        KeyValueStore accessTokenStore = storeProvider.getStore(ACCESS_TOKEN);
        String tokenString = RandomUtils.generateRandomString(16, config.getSecureRandom());
        AccessToken accessToken = builder(new AccessToken())
                .set(AccessToken::setActive, true)
                .set(AccessToken::setClientId, clientId)
                //.set(AccessToken::setScope, )
                .set(AccessToken::setSub, account)
                .build();
        accessTokenStore.write(tokenString, accessToken);
        return tokenString;
    }

    private String createAccessToken(User user, String clientId) {
        return createAccessToken(user.getAccount(), clientId);
    }

    private String makeCallbackUrl(String baseUrl, Parameters params) {
        String encoded = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + CodecUtils.urlEncode(e.getValue().toString()))
                .collect(Collectors.joining("&"));
        return baseUrl.contains("?") ? baseUrl + "&" + encoded : baseUrl + "?" + encoded;
    }

    /**
     * Authorization endpoint.
     *
     * @param params    Request parameters
     * @param principal User principal
     * @param request   HttpRequest object
     * @return Authorization response
     */
    public HttpResponse authorize(Parameters params, UserPermissionPrincipal principal, HttpRequest request) {
        if (principal != null) {
            Parameters responseParams = Parameters.of();
            if (params.containsKey("state")) {
                responseParams.put("state", params.get("state"));
            }

            String clientId = params.get("client_id");
            OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
            OidcApplication oidcApplication = oidcApplicationDao.selectByClientId(clientId);
            String redirectUrl = (String) params.getOrDefault("redirect_url", oidcApplication.getCallbackUrl());

            Set<ResponseType> responseTypes = Arrays.stream(((String) params.getOrDefault("response_type", "code")).split("[ ,]+"))
                    .map(rt -> ResponseType.of(rt))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (responseTypes.contains(ID_TOKEN)) {
                String nonce = params.get("nonce");
                responseParams.put("id_token", createIdToken(principal.getId(), oidcApplication, nonce));
            }
            if (responseTypes.contains(TOKEN)) {
                responseParams.put("access_token", createAccessToken(principal.getName(), clientId));
                responseParams.put("token_type", "bearer");
                responseParams.put("expires_in", 3600);
            }
            if (responseTypes.contains(CODE)) {
                KeyValueStore authorizationCodeStore = storeProvider.getStore(AUTHORIZATION_CODE);
                String code = RandomUtils.generateRandomString(16, config.getSecureRandom());
                authorizationCodeStore.write(code, principal.getId());
                responseParams.put("code", code);
            }
            return HttpResponseUtils.redirect(makeCallbackUrl(redirectUrl, responseParams), FOUND);
        } else {
            return HttpResponseUtils.redirect("/my/signIn?url=" + request.getUri() + "?" + CodecUtils.urlEncode(request.getQueryString()), SEE_OTHER);
        }
    }

    private String[] getClientIdAndSecret(HttpRequest request, Parameters params) {
        return Optional.ofNullable(request.getHeaders().get("Authorization"))
                .filter(authz ->  authz.startsWith("Basic"))
                .map(   authz ->  authz.split("\\s+", 2))
                .filter(tokens -> tokens.length == 2)
                .map(   tokens -> Base64.getUrlDecoder().decode(tokens[1]))
                .map(   bytes ->  new String(bytes).split(":", 2))
                .filter(idAndSecret -> idAndSecret.length == 2)
                .orElseGet(() -> new String[]{ params.get("client_id"), params.get("client_secret")});
    }

    /**
     * A token endpoint.
     *
     * @param request HttpRequest object
     * @param params  Request parameters
     * @return Token response
     */
    public HttpResponse token(HttpRequest request, Parameters params) {
        String grantType = params.get("grant_type");
        UserDao userDao = daoProvider.getDao(UserDao.class);

        User user = null;
        if (Objects.equals(grantType, "client_credentials")) {
            String account = some(request.getHeaders().get("X-Client-DN"),
                    clientDN -> new X500Name(clientDN).getRDNs(BCStyle.CN)[0],
                    cn -> IETFUtils.valueToString(cn.getFirst().getValue())).orElse(null);
            user = userDao.selectByAccount(account);
        } else if (Objects.equals(grantType, "code")) {
            KeyValueStore authorizationCodeStore = storeProvider.getStore(AUTHORIZATION_CODE);
            Long userId = (Long) authorizationCodeStore.read(params.get("code"));
            if (userId == null) {
                return oauthService.errorResponse(INVALID_CLIENT, "code is not authorized", null);
            }
            authorizationCodeStore.delete(params.get("code"));
            user = userDao.selectById(userId);
        } else {
            return oauthService.errorResponse(UNSUPPORTED_GRANT_TYPE);
        }

        String[] clientIdAndSecret = getClientIdAndSecret(request, params);
        String clientId = clientIdAndSecret[0];
        String clientSecret = clientIdAndSecret[1];

        OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        OidcApplication oidcApplication = oidcApplicationDao.selectByClientId(clientId);
        if (!Objects.equals(oidcApplication.getClientSecret(), clientSecret)) {
            return oauthService.errorResponse(UNAUTHORIZED_CLIENT);
        }
        // Access Token
        String token = createAccessToken(user, clientId);
        String nonce = params.get("nonce");
        String idTokenSigned = createIdToken(user.getId(), oidcApplication, nonce);

        HttpResponse response = HttpResponseUtils.response("{"
                + "\"access_token\":\""+ token + "\","
                + "\"token_type\":\"Bearer\","
                + "\"expires_in\":3600,"
                + "\"id_token\":\"" + idTokenSigned + "\""
                + "}");
        return some(response,
                res -> contentType(res, "application/json")).orElse(null);
    }

    public HttpResponse introspect(HttpRequest request, Parameters params) {
        String token = params.get("token");
        KeyValueStore accessTokenStore = storeProvider.getStore(ACCESS_TOKEN);
        AccessToken accessToken = (AccessToken) accessTokenStore.read(token);

        if (accessToken == null) {
            return some(HttpResponseUtils.response("{\"active\":false}"),
                    res -> contentType(res, "application/json")).orElse(null);
        } else {
            HttpResponse response = HttpResponseUtils.response("{"
                    + "\"sub\":\"" + accessToken.getSub() + "\","
                    + "\"scope\":\"" + accessToken.getScope() + "\","
                    + "\"client_id\":\"" + accessToken.getClientId() + "\","
                    + "}");
            return some(response,
                    res -> contentType(res, "application/json")).orElse(null);
        }
    }
}
