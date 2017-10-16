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
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtHeader;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.entity.ResponseType;
import net.unit8.bouncr.web.dao.OidcApplicationDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.OidcApplication;
import net.unit8.bouncr.web.entity.User;

import javax.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.FOUND;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.*;
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

    private String createAccessToken() {
        KeyValueStore accessTokenStore = storeProvider.getStore(ACCESS_TOKEN);
        String accessToken = RandomUtils.generateRandomString(16, config.getSecureRandom());
        accessTokenStore.write(accessToken, accessToken);
        return accessToken;
    }

    private String makeCallbackUrl(String baseUrl, Parameters params) {
        String encoded = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + CodecUtils.urlEncode(e.getValue().toString()))
                .collect(Collectors.joining("&"));
        return baseUrl.contains("?") ? baseUrl + "&" + encoded : baseUrl + "?" + encoded;
    }

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
                responseParams.put("access_token", createAccessToken());
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

    public HttpResponse token(HttpRequest request, Parameters params) {
        KeyValueStore authorizationCodeStore = storeProvider.getStore(AUTHORIZATION_CODE);
        Long userId = (Long) authorizationCodeStore.read(params.get("code"));
        if (userId == null) {
            return builder(HttpResponseUtils.response("code is not authorized"))
                    .set(HttpResponse::setStatus, 401)
                    .build();
        }
        authorizationCodeStore.delete(params.get("code"));

        String[] clientIdAndSecret = getClientIdAndSecret(request, params);
        String clientId = clientIdAndSecret[0];
        String clientSecret = clientIdAndSecret[1];

        OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        OidcApplication oidcApplication = oidcApplicationDao.selectByClientId(clientId);
        if (!Objects.equals(oidcApplication.getClientSecret(), clientSecret)) {
            return builder(HttpResponseUtils.response("client secret is not authorized"))
                    .set(HttpResponse::setStatus, 401)
                    .build();
        }
        // Access Token
        String nonce = params.get("nonce");
        String idTokenSigned = createIdToken(userId, oidcApplication, nonce);
        String accessToken = createAccessToken();

        HttpResponse response = HttpResponseUtils.response("{"
                + "\"access_token\":\""+ accessToken + "\","
                + "\"token_type\":\"Bearer\","
                + "\"expires_in\":3600,"
                + "\"id_token\":\"" + idTokenSigned + "\""
                + "}");
        return some(response,
                res -> HttpResponseUtils.contentType(res, "application/json")).orElse(null);
    }
}
