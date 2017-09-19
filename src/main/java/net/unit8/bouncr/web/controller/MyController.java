package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.doma2.DomaProvider;
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
import org.seasar.doma.jdbc.SelectOptions;

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

    public HttpResponse home(UserPermissionPrincipal principal, HttpRequest request) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectByAccount(principal.getName());

        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        List<UserAction> userActions = auditDao
                .selectForConditionalSearch(null, null, user.getAccount(),
                        SelectOptions.get().limit(10));
        String token = some(request.getCookies().get(config.getTokenName()),
                cookie -> cookie.getValue())
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

    public HttpResponse account(UserPermissionPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(principal.getId());

        ChangePasswordForm form = new ChangePasswordForm();
        OtpKey otpKey = userDao.selectOtpKeyById(user.getId());
        String twofaSecret = null;
        if (otpKey != null) {
            twofaSecret = Base32Utils.encode(otpKey.getKey());
        }
        return templateEngine.render("my/account",
                "user", user,
                "changePassword", form,
                "twofaSecret", twofaSecret);
    }

    @Transactional
    public HttpResponse changePassword(ChangePasswordForm form, UserPermissionPrincipal principal, HttpRequest request) {
        if (form.hasErrors()) {
            return templateEngine.render("my/account",
                    "user", form);
        } else {
            UserDao userDao = daoProvider.getDao(UserDao.class);
            User user = userDao.selectByPassword(principal.getName(), form.getOldPassword());
            if (user == null) {
                form = new ChangePasswordForm();
                return templateEngine.render("my/account",
                        "message", "error.oldPasswordMismatch",
                        "user", form);
            }

            PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
            String salt = RandomUtils.generateRandomString(16);
            passwordCredentialDao.update(builder(new PasswordCredential())
                            .set(PasswordCredential::setId, user.getId())
                            .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(form.getNewPassword(), salt, 100))
                            .set(PasswordCredential::setSalt, salt)
                            .build());
            userDao.update(user);

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
                    .set(OtpKey::setKey, RandomUtils.generateRandomString(20).getBytes())
                    .build());
        } else {
            otpKeyDao.delete(builder(new OtpKey())
                    .set(OtpKey::setUserId, principal.getId())
                    .build());
        }

        return UrlRewriter.redirect(MyController.class, "account", SEE_OTHER);
    }
}
