package net.unit8.bouncr.web.controller;

import enkan.collection.Multimap;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.util.BeanBuilder;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.AuditDao;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.PermissionWithRealm;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.LoginForm;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.HttpResponseUtils.redirect;
import static enkan.util.ThreadingUtils.some;

public class LoginController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private StoreProvider storeProvider;

    private String getAccountFromClientDN(HttpRequest request) {
        String clientDN = request.getHeaders().get("X-Client-DN");
        RDN cn = new X500Name(clientDN).getRDNs(BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }

    public HttpResponse loginForm(HttpRequest request, LoginForm form) {
        String account = getAccountFromClientDN(request);
        if (account != null) {
            return templateEngine.render("my/login/clientdn",
                    "account", account,
                    "url", form.getUrl());
        } else {
            return templateEngine.render("my/login/index",
                    "url", form.getUrl());
        }
    }

    public HttpResponse loginByPassword(LoginForm form) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user= userDao.selectByPassword(form.getAccount(), form.getPassword());
        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        auditDao.signin(form.getAccount(), user != null);

        if (user != null) {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            Map<Long, UserPermissionPrincipal> permsByRealm = permissionDao
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
            String token = UUID.randomUUID().toString();
            storeProvider.getStore().write(token, new HashMap<>(permsByRealm));

            Cookie tokenCookie = Cookie.create("BOUNCR_TOKEN", token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            String redirectUrl = Optional.ofNullable(form.getUrl()).orElse("/my");
            return BeanBuilder.builder(redirect(redirectUrl, HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                    .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                    .build();
        } else {
            return templateEngine.render("my/login/index");
        }
    }

    public HttpResponse loginByClientDN(HttpRequest request, LoginForm form) {
        form.setAccount(getAccountFromClientDN(request));
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user= userDao.selectByAccount(form.getAccount());
        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        auditDao.signin(form.getAccount(), user != null);

        if (user != null) {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            Map<Long, UserPermissionPrincipal> permsByRealm = permissionDao
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
            String token = UUID.randomUUID().toString();
            storeProvider.getStore().write(token, new HashMap<>(permsByRealm));

            Cookie tokenCookie = Cookie.create("BOUNCR_TOKEN", token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            String redirectUrl = Optional.ofNullable(form.getUrl()).orElse("/my");
            return BeanBuilder.builder(redirect(redirectUrl, HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                    .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                    .build();
        } else {
            return templateEngine.render("my/login/clientdn",
                    "account", form.getAccount(),
                    "url", form.getUrl());
        }
    }

    public HttpResponse logout(HttpRequest request) {
        some(request.getCookies().get("BOUNCR_TOKEN"),
                Cookie::getValue).ifPresent(
                token -> storeProvider.getStore().delete(token)
        );
        Cookie expire = builder(Cookie.create("BOUNCR_TOKEN", ""))
                .set(Cookie::setPath, "/")
                .set(Cookie::setMaxAge, -1)
                .build();
        return (HttpResponse<String>) builder(UrlRewriter.redirect(LoginController.class, "loginForm", SEE_OTHER))
                .set(HttpResponse::setCookies, Multimap.of("BOUNCR_TOKEN", expire))
                .build();
    }
}
