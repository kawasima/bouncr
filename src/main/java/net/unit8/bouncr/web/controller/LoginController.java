package net.unit8.bouncr.web.controller;

import enkan.collection.Multimap;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpResponse;
import enkan.util.BeanBuilder;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.PermissionWithRealm;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.LoginForm;

import javax.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.redirect;

public class LoginController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private StoreProvider storeProvider;

    public HttpResponse loginForm() {
        return templateEngine.render("my/login/index");
    }

    public HttpResponse loginByPassword(LoginForm form) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user= userDao.selectByPassword(form.getAccount(), form.getPassword());
        if (user != null) {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            Map<Long, UserPermissionPrincipal> permsByRealm = permissionDao
                    .selectByUserId(user.getId())
                    .stream()
                    .collect(Collectors.groupingBy(p -> p.getRealmId()))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e ->
                            new UserPermissionPrincipal(
                                    user.getAccount(),
                                    e.getValue().stream()
                                            .map(p -> p.getPermission())
                                            .collect(Collectors.toSet()))));
            String token = UUID.randomUUID().toString();
            storeProvider.getStore().write(token, new HashMap<>(permsByRealm));

            Cookie tokenCookie = Cookie.create("BOUNCR_TOKEN", token);
            tokenCookie.setPath("/");
            tokenCookie.setHttpOnly(true);
            return BeanBuilder.builder(redirect("/my", HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                    .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                    .build();
        } else {
            return templateEngine.render("my/login/index");
        }
    }
}
