package net.unit8.bouncr.web.controller;

import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.web.dao.ApplicationDao;
import net.unit8.bouncr.web.dao.AuditDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Application;
import net.unit8.bouncr.web.entity.SignInHistory;
import net.unit8.bouncr.web.entity.User;
import org.seasar.doma.jdbc.SelectOptions;

import javax.inject.Inject;
import java.util.List;

public class MyController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    public HttpResponse home(UserPermissionPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectByAccount(principal.getName());

        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        List<SignInHistory> signInHistories = auditDao
                .selectForConditionalSearch(null, null, user.getAccount(), null,
                        SelectOptions.get().limit(10));

        ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
        List<Application> applications = applicationDao.selectByUserId(user.getId());
        return templateEngine.render("my/home",
                "user", user,
                "applications", applications,
                "signInHistories", signInHistories);
    }
}
