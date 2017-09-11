package net.unit8.bouncr.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import enkan.collection.Multimap;
import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.util.BeanBuilder;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.LdapClient;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.AuditDao;
import net.unit8.bouncr.web.dao.OAuth2ProviderDao;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.ActionType;
import net.unit8.bouncr.web.entity.OAuth2Provider;
import net.unit8.bouncr.web.entity.PermissionWithRealm;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.SignInForm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.UniqueConstraintException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.HttpResponseUtils.redirect;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.web.entity.ActionType.*;

public class SignInController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private LdapClient ldapClient;

    @Inject
    private BouncrConfiguration config;

    public HttpResponse signInForm(HttpRequest request, SignInForm form) {
        String account = getAccountFromClientDN(request);
        if (account != null) {
            return templateEngine.render("my/signIn/clientdn",
                    "account", account,
                    "signin", form);
        } else {
            OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
            String callbackUrl = request.getScheme() + "://" + request.getServerName() + ":" +request.getServerPort()
                    + "/my/signIn/oauth";
            List<OAuth2ProviderDto> oauth2Providers = oauth2ProviderDao
                    .selectAll()
                    .stream()
                    .map(p -> new OAuth2ProviderDto(p,
                            getOAuth20Service(p, callbackUrl + "?id=" + p.getId())
                                    .getAuthorizationUrl()))
                    .collect(Collectors.toList());

            return templateEngine.render("my/signIn/index",
                    "oauth2Providers", oauth2Providers,
                    "signin", form);
        }
    }

    @Data
    @AllArgsConstructor
    public static class OAuth2ProviderDto implements Serializable {
        private OAuth2Provider oauth2Provider;
        private String authorizationUrl;
    }

    private void recordSignIn(User user, HttpRequest request, SignInForm form) {
        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        auditDao.insertUserAction(user != null ? USER_SIGNIN : USER_FAILED_SIGNIN,
                form.getAccount(),
                request.getRemoteAddr());

        if (user == null) {
            UserDao userDao = daoProvider.getDao(UserDao.class);
            user = userDao.selectByAccount(form.getAccount());
            if (user != null) {
                List<String> actionTypes = auditDao.selectRecentSigninHistories(form.getAccount(),
                        SelectOptions.get().limit(config.getPasswordPolicy().getNumOfTrialsUntilLock()));
                if (actionTypes.stream().filter(t -> t.equals("user.failed_signin")).count() >= config.getPasswordPolicy().getNumOfTrialsUntilLock()) {
                    try {
                        userDao.lock(user.getId());
                    } catch(UniqueConstraintException ignore) {

                    }
                }
            }
        }
    }

    @Transactional
    public HttpResponse signInByPassword(HttpRequest request, SignInForm form) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        if (userDao.isLock(form.getAccount())) {
            form.setErrors(Multimap.of("account", "error.accountLocked"));
            return signInForm(request, form);
        }
        User user= userDao.selectByPassword(form.getAccount(), form.getPassword());

        if (user == null && ldapClient != null) {
            if (ldapClient.search(form.getAccount(), form.getPassword())) {
                user = userDao.selectByAccount(form.getAccount());
            }
        }

        recordSignIn(user, request, form);

        if (user != null) {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            String token = UUID.randomUUID().toString();
            storeProvider.getStore(BOUNCR_TOKEN).write(token, new HashMap<>(getPermissionsByRealm(user, permissionDao)));

            Cookie tokenCookie = Cookie.create(config.getTokenName(), token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            String redirectUrl = Optional.ofNullable(form.getUrl()).orElse("/my");
            return BeanBuilder.builder(redirect(redirectUrl, HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                    .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                    .build();
        } else {
            form.setErrors(Multimap.of("account", "error.failToSignin"));
            return signInForm(request, form);
        }
    }

    @Transactional
    public HttpResponse signInByClientDN(HttpRequest request, SignInForm form) {
        form.setAccount(getAccountFromClientDN(request));
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user= userDao.selectByAccount(form.getAccount());
        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        auditDao.insertUserAction(user != null?USER_SIGNIN:USER_FAILED_SIGNIN, form.getAccount(), request.getRemoteAddr());

        if (user != null) {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            String token = UUID.randomUUID().toString();
            storeProvider.getStore(BOUNCR_TOKEN).write(token, new HashMap<>(getPermissionsByRealm(user, permissionDao)));

            Cookie tokenCookie = Cookie.create(config.getTokenName(), token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            String redirectUrl = Optional.ofNullable(form.getUrl()).orElse("/my");
            return BeanBuilder.builder(redirect(redirectUrl, HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                    .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                    .build();
        } else {
            return templateEngine.render("my/signIn/clientdn",
                    "signin", form);
        }
    }

    public HttpResponse signInByOAuth(HttpRequest request, Parameters params) {
        OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
        UserDao userDao = daoProvider.getDao(UserDao.class);
        OAuth2Provider oauth2Provider = oauth2ProviderDao.selectById(params.getLong("id"));
        OAuth20Service oauthService = getOAuth20Service(oauth2Provider, "");
        Future<OAuth2AccessToken> accessTokenFuture = oauthService.getAccessToken(params.get("code"), new OAuthAsyncRequestCallback<OAuth2AccessToken>() {
            @Override
            public void onCompleted(OAuth2AccessToken oAuth2AccessToken) {

            }

            @Override
            public void onThrowable(Throwable throwable) {

            }
        });
        OAuth2AccessToken accessToken;
        try {
            accessToken = accessTokenFuture.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return builder(HttpResponseUtils.response("Cannot fetch the access token."))
                    .set(HttpResponse::setStatus, 503)
                    .build();
        }
        OkHttpClient okhttp = new OkHttpClient();
        try {
            ObjectMapper jsonMapper = new ObjectMapper();

            InputStream in = okhttp.newCall(new Request.Builder()
                    .url(oauth2Provider.getUserInfoEndpoint())
                    .header("Authorization", "token " + accessToken.getAccessToken())
                    .build())
                    .execute()
                    .body()
                    .byteStream();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
            HashMap<String, Object> map = jsonMapper.readValue(in, typeRef);
            Object id = map.get(oauth2Provider.getUserIdPath());
            if (id != null) {
                User user = userDao.selectByOAuth2(oauth2Provider.getId(), Objects.toString(id));
                // TODO
                if (user == null) {

                } else {

                }
            }
        } catch (IOException e) {
            return builder(HttpResponseUtils.response("Cannot fetch the user information from the resource server."))
                    .set(HttpResponse::setStatus, 503)
                    .build();
        }
        return HttpResponse.of("OK");
    }

    public HttpResponse logout(HttpRequest request) {
        some(request.getCookies().get(config.getTokenName()),
                Cookie::getValue).ifPresent(
                token -> storeProvider.getStore(BOUNCR_TOKEN).delete(token)
        );
        Cookie expire = builder(Cookie.create(config.getTokenName(), ""))
                .set(Cookie::setPath, "/")
                .set(Cookie::setMaxAge, -1)
                .build();
        return (HttpResponse<String>) builder(UrlRewriter.redirect(SignInController.class, "signInForm", SEE_OTHER))
                .set(HttpResponse::setCookies, Multimap.of(config.getTokenName(), expire))
                .build();
    }

    private Map<Long, UserPermissionPrincipal> getPermissionsByRealm(User user, PermissionDao permissionDao) {
        return permissionDao
                .selectByUserId(user.getId())
                .stream()
                .collect(Collectors.groupingBy(PermissionWithRealm::getRealmId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        new UserPermissionPrincipal(
                                user.getAccount(),
                                e.getValue().stream()
                                        .map(PermissionWithRealm::getPermission)
                                        .collect(Collectors.toSet()))));
    }

    private String getAccountFromClientDN(HttpRequest request) {
        return some(request.getHeaders().get("X-Client-DN"),
                clientDN -> new X500Name(clientDN).getRDNs(BCStyle.CN)[0],
                cn -> IETFUtils.valueToString(cn.getFirst().getValue())).orElse(null);
    }

    private OAuth20Service getOAuth20Service(OAuth2Provider oauth2Provider, String callbackUrl) {
        ServiceBuilder oauth2builder = new ServiceBuilder(oauth2Provider.getApiKey());
        if (oauth2Provider.getApiSecret() != null) oauth2builder = oauth2builder.apiSecret(oauth2Provider.getApiSecret());
        oauth2builder = oauth2builder.callback(callbackUrl);
        return oauth2builder.build(new DefaultApi20() {
            @Override
            public String getAccessTokenEndpoint() {
                return oauth2Provider.getAccessTokenEndpoint();
            }

            @Override
            protected String getAuthorizationBaseUrl() {
                return oauth2Provider.getAuthorizationBaseUrl();
            }

            @Override
            public Verb getAccessTokenVerb() {
                return Verb.POST;
            }

            @Override
            public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
                return OAuth2AccessTokenExtractor.instance();
            }
        });
    }
}
