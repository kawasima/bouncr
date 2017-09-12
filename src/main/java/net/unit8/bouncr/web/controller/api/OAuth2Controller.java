package net.unit8.bouncr.web.controller.api;

import enkan.collection.Parameters;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.session.KeyValueStore;
import enkan.security.UserPrincipal;
import enkan.util.CodecUtils;
import enkan.util.HttpRequestUtils;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.OAuth2ApplicationDao;
import net.unit8.bouncr.web.entity.OAuth2Application;

import javax.inject.Inject;

import java.util.Objects;
import java.util.Random;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static net.unit8.bouncr.component.StoreProvider.StoreType.*;

public class OAuth2Controller {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private StoreProvider storeProvider;

    private Random random = new Random();

    public HttpResponse authorize(Parameters params, UserPrincipal principal, HttpRequest request) {
        if (principal != null) {
            String clientId = params.get("client_id");
            OAuth2ApplicationDao oauth2ApplicationDao = daoProvider.getDao(OAuth2ApplicationDao.class);
            OAuth2Application oauth2Application = oauth2ApplicationDao.selectByClientId(clientId);
            String redirectUrl = (String) params.getOrDefault("redirect_url", oauth2Application.getCallbackUrl());

            KeyValueStore authorizationCodeStore = storeProvider.getStore(AUTHORIZATION_CODE);
            String code = RandomUtils.generateRandomString(random, 16);
            authorizationCodeStore.write(code, (String) params.getOrDefault("state", "STATE"));

            if (redirectUrl.contains("?")) {
                redirectUrl += "&code=" + code;
            } else {
                redirectUrl += "?code=" + code;
            }
            return HttpResponseUtils.redirect(redirectUrl, SEE_OTHER);
        } else {
            return HttpResponseUtils.redirect("/my/signIn?url=" + request.getUri() + "?" + CodecUtils.urlEncode(request.getQueryString()), SEE_OTHER);
        }
    }

    public HttpResponse accessToken(Parameters params) {
        KeyValueStore authorizationCodeStore = storeProvider.getStore(AUTHORIZATION_CODE);
        String state = (String) authorizationCodeStore.read(params.get("code"));
        if (state == null) {
            return builder(HttpResponseUtils.response("code is not authorized"))
                    .set(HttpResponse::setStatus, 401)
                    .build();
        }
        authorizationCodeStore.delete(params.get("code"));

        OAuth2ApplicationDao oauth2ApplicationDao = daoProvider.getDao(OAuth2ApplicationDao.class);
        OAuth2Application oauth2Application = oauth2ApplicationDao.selectByClientId(params.get("client_id"));
        if (!Objects.equals(oauth2Application.getClientSecret(), params.get("client_secret"))) {
            return builder(HttpResponseUtils.response("client secret is not authorized"))
                    .set(HttpResponse::setStatus, 401)
                    .build();
        }

        KeyValueStore accessTokenStore = storeProvider.getStore(ACCESS_TOKEN);
        String accessToken = RandomUtils.generateRandomString(random, 16);
        accessTokenStore.write(accessToken, accessToken);

        String redirectUrl = (String) params.getOrDefault("redirect_url", oauth2Application.getCallbackUrl());
        if (redirectUrl.contains("?")) {
            redirectUrl += "&access_token=" + accessToken;
        } else {
            redirectUrl += "?access_token=" + accessToken;
        }

        return HttpResponseUtils.redirect(redirectUrl, SEE_OTHER);
    }
}
