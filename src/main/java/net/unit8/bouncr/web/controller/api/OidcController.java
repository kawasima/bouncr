package net.unit8.bouncr.web.controller.api;

import enkan.collection.Parameters;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.session.KeyValueStore;
import enkan.security.UserPrincipal;
import enkan.util.CodecUtils;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.sign.IdToken;
import net.unit8.bouncr.sign.IdTokenHeader;
import net.unit8.bouncr.sign.IdTokenPayload;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.OidcApplicationDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.OidcApplication;
import net.unit8.bouncr.web.entity.User;

import javax.inject.Inject;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.*;

public class OidcController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private IdToken idToken;

    @Inject
    private BouncrConfiguration config;

    public HttpResponse authorize(Parameters params, UserPermissionPrincipal principal, HttpRequest request) {
        if (principal != null) {
            String clientId = params.get("client_id");
            OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
            OidcApplication oidcApplication = oidcApplicationDao.selectByClientId(clientId);
            String redirectUrl = (String) params.getOrDefault("redirect_url", oidcApplication.getCallbackUrl());

            KeyValueStore authorizationCodeStore = storeProvider.getStore(AUTHORIZATION_CODE);
            String code = RandomUtils.generateRandomString(16, config.getSecureRandom());
            authorizationCodeStore.write(code, principal.getId());

            if (redirectUrl.contains("?")) {
                redirectUrl += "&code=" + code;
            } else {
                redirectUrl += "?code=" + code;
            }
            redirectUrl += "&state=" + params.getOrDefault("state", "STATE")
                    + "&session_state=session_state";
            return HttpResponseUtils.redirect(redirectUrl, SEE_OTHER);
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

        KeyValueStore accessTokenStore = storeProvider.getStore(ACCESS_TOKEN);
        String accessToken = RandomUtils.generateRandomString(16, config.getSecureRandom());
        accessTokenStore.write(accessToken, accessToken);

        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(userId);
        String idTokenSigned = idToken.sign(builder(new IdTokenPayload())
                        .set(IdTokenPayload::setSub, user.getId().toString())
                        .set(IdTokenPayload::setIss, "")
                        .set(IdTokenPayload::setAud, oidcApplication.getClientId())
                        .set(IdTokenPayload::setEmail, user.getEmail())
                        .set(IdTokenPayload::setName, user.getName())
                        .set(IdTokenPayload::setPreferredUsername, user.getAccount())
                        .set(IdTokenPayload::setIat, (System.currentTimeMillis() / 1000) - 60)
                        .set(IdTokenPayload::setExp, (System.currentTimeMillis() / 1000) + 3600)
                        .build(),
                builder(new IdTokenHeader())
                        .set(IdTokenHeader::setAlg, "RS256")
                        .set(IdTokenHeader::setKid, "")
                        .build(),
                KeyUtils.decode(oidcApplication.getPrivateKey()));

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
