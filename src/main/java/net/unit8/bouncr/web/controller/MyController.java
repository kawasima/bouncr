package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.util.Base32Utils;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.*;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.ChangePasswordForm;
import net.unit8.bouncr.web.form.TwoFactorAuthForm;
import net.unit8.bouncr.web.service.PasswordCredentialService;
import org.seasar.doma.jdbc.SelectOptions;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;

public class MyController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    private PasswordCredentialService passwordCredentialService;

    @PostConstruct
    private void initialize() {
        passwordCredentialService = new PasswordCredentialService(daoProvider, config);
    }

    public HttpResponse home(UserPermissionPrincipal principal, HttpRequest request) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectByAccount(principal.getName());

        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        List<UserAction> userActions = auditDao
                .selectForConditionalSearch(null, null, user.getAccount(),
                        SelectOptions.get().limit(10));
        String token = some(request.getCookies().get(config.getTokenName()),
                Cookie::getValue)
                .orElse(null);
        UserSessionDao userSessionDao = daoProvider.getDao(UserSessionDao.class);
        List<UserSession> userSessions = userSessionDao.selectByUserId(user.getId());

        ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
        List<Application> applications = applicationDao.selectByUserId(user.getId());

        return templateEngine.render("my/home",
                "user", user,
                "token", token,
                "userSessions", userSessions,
                "applications", applications,
                "userActions", userActions);
    }

    public HttpResponse application(UserPermissionPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectByAccount(principal.getName());

        ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
        List<Application> applications = applicationDao.selectByUserId(user.getId());

        return templateEngine.render("my/application",
                "user", user,
                "applications", applications);
    }

    public HttpResponse account(UserPermissionPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(principal.getId());

        ChangePasswordForm form = new ChangePasswordForm();
        return templateEngine.render("my/account",
                "passwordEnabled", config.isPasswordEnabled(),
                "user", user,
                "changePassword", form,
                "twofaSecret", twoFactorAuthenticationSecret(user));
    }

    @Transactional
    public HttpResponse changePassword(ChangePasswordForm form, UserPermissionPrincipal principal, HttpRequest request) {
        passwordCredentialService.validateBasedOnPasswordPolicy(form, "newPassword");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(principal.getId());

        if (form.hasErrors()) {
            return templateEngine.render("my/account",
                    "passwordEnabled", config.isPasswordEnabled(),
                    "user", user,
                    "changePassword", form,
                    "twofaSecret", twoFactorAuthenticationSecret(user));
        } else {
            User credUser = userDao.selectByPassword(principal.getName(), form.getOldPassword());
            if (credUser == null) {
                form = new ChangePasswordForm();
                return templateEngine.render("my/account",
                        "passwordEnabled", config.isPasswordEnabled(),
                        "message", "error.oldPasswordMismatch",
                        "user", user,
                        "changePassword", form,
                        "twofaSecret", twoFactorAuthenticationSecret(user));
            }

            passwordCredentialService.changePassword(user, form.getNewPassword());
            AuditDao auditDao = daoProvider.getDao(AuditDao.class);
            auditDao.insertUserAction(ActionType.CHANGE_PASSWORD, user.getAccount(), request.getRemoteAddr());

            return UrlRewriter.redirect(MyController.class, "home", SEE_OTHER);
        }
    }

    @Transactional
    public HttpResponse revokeSession(Parameters params, UserPermissionPrincipal principal) {

        UserSessionDao userSessionDao = daoProvider.getDao(UserSessionDao.class);
        UserSession session = userSessionDao.selectById(params.getLong("id"), principal);

        storeProvider.getStore(BOUNCR_TOKEN).delete(session.getToken());
        userSessionDao.delete(session);

        return UrlRewriter.redirect(MyController.class, "home", SEE_OTHER);
    }

    @Transactional
    public HttpResponse switchTwoFactorAuth(UserPermissionPrincipal principal, TwoFactorAuthForm form) {
        OtpKeyDao otpKeyDao = daoProvider.getDao(OtpKeyDao.class);
        if (Objects.equals(form.getEnabled(), "on")) {
            otpKeyDao.insert(builder(new OtpKey())
                    .set(OtpKey::setUserId, principal.getId())
                    .set(OtpKey::setKey, RandomUtils.generateRandomString(20, config.getSecureRandom()).getBytes())
                    .build());
        } else {
            otpKeyDao.delete(builder(new OtpKey())
                    .set(OtpKey::setUserId, principal.getId())
                    .build());
        }

        return UrlRewriter.redirect(MyController.class, "account", SEE_OTHER);
    }

    private String twoFactorAuthenticationSecret(User user) {
        String twofaSecret = null;
        UserDao userDao = daoProvider.getDao(UserDao.class);
        OtpKey otpKey = userDao.selectOtpKeyById(user.getId());

        if (otpKey != null) {
            twofaSecret = Base32Utils.encode(otpKey.getKey());
        }
        return twofaSecret;
    }
}
