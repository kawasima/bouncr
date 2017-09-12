package net.unit8.bouncr.web.controller;

import enkan.component.doma2.DomaProvider;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.*;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.ChangePasswordForm;
import org.seasar.doma.jdbc.SelectOptions;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Random;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.*;

public class MyController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    public HttpResponse home(UserPermissionPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectByAccount(principal.getName());

        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        List<UserAction> userActions = auditDao
                .selectForConditionalSearch(null, null, user.getAccount(),
                        SelectOptions.get().limit(10));

        UserSessionDao userSessionDao = daoProvider.getDao(UserSessionDao.class);
        List<UserSession> userSessions = userSessionDao.selectByUserId(user.getId());

        ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
        List<Application> applications = applicationDao.selectByUserId(user.getId());

        return templateEngine.render("my/home",
                "user", user,
                "userSessions", userSessions,
                "applications", applications,
                "userActions", userActions);
    }

    public HttpResponse account() {
        ChangePasswordForm form = new ChangePasswordForm();
        return templateEngine.render("my/account",
                "user", form);
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
            Random random = new Random();
            passwordCredentialDao.update(
                    user.getId(),
                    form.getNewPassword(),
                    RandomUtils.generateRandomString(random, 16));
            userDao.update(user);

            AuditDao auditDao = daoProvider.getDao(AuditDao.class);
            auditDao.insertUserAction(ActionType.CHANGE_PASSWORD, user.getAccount(), request.getRemoteAddr());

            return UrlRewriter.redirect(MyController.class, "home", SEE_OTHER);
        }
    }
}
