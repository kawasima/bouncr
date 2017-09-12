package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.OAuth2ApplicationDao;
import net.unit8.bouncr.web.entity.OAuth2Application;
import net.unit8.bouncr.web.form.OAuth2ApplicationForm;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Random;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

public class OAuth2ApplicationController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @RolesAllowed("LIST_OAUTH2_APPLICATIONS")
    public HttpResponse list() {
        OAuth2ApplicationDao OAuth2ApplicationDao = daoProvider.getDao(OAuth2ApplicationDao.class);
        List<OAuth2Application> oauth2Applications =OAuth2ApplicationDao.selectAll();

        return templateEngine.render("admin/oauth2Application/list",
                "oauth2Applications", oauth2Applications);
    }

    @RolesAllowed("CREATE_OAUTH2_APPLICATION")
    public HttpResponse newForm() {
        return templateEngine.render("admin/oauth2Application/new",
                "oauth2Application", new OAuth2ApplicationForm());
    }

    @RolesAllowed("MODIFY_OAUTH2_APPLICATION")
    public HttpResponse edit(Parameters params) {
        OAuth2ApplicationDao OAuth2ApplicationDao = daoProvider.getDao(OAuth2ApplicationDao.class);
        OAuth2Application OAuth2Application = OAuth2ApplicationDao.selectById(params.getLong("id"));
        OAuth2ApplicationForm form = beansConverter.createFrom(OAuth2Application, OAuth2ApplicationForm.class);
        return templateEngine.render("admin/oauth2Application/edit",
                "oauth2Application", form);
    }

    @RolesAllowed("CREATE_OAUTH2_APPLICATION")
    @Transactional
    public HttpResponse create(OAuth2ApplicationForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oauth2Application/new",
                    "oauth2Application", form);
        } else {
            OAuth2ApplicationDao oauth2ApplicationDao = daoProvider.getDao(OAuth2ApplicationDao.class);
            OAuth2Application oauth2Application = beansConverter.createFrom(form, OAuth2Application.class);
            Random random = new Random();
            oauth2Application.setClientId(RandomUtils.generateRandomString(random, 16));
            oauth2Application.setClientSecret(RandomUtils.generateRandomString(random, 32));
            oauth2ApplicationDao.insert(oauth2Application);
            return UrlRewriter.redirect(OAuth2ApplicationController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed("MODIFY_OAUTH2_APPLICATION")
    @Transactional
    public HttpResponse update(OAuth2ApplicationForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oauth2Application/edit",
                    "OAuth2Application", form);
        } else {
            OAuth2ApplicationDao oauth2ApplicationDao = daoProvider.getDao(OAuth2ApplicationDao.class);
            OAuth2Application oauth2Application = oauth2ApplicationDao.selectById(form.getId());
            beansConverter.copy(form, oauth2Application);
            oauth2ApplicationDao.update(oauth2Application);
            return UrlRewriter.redirect(OAuth2ApplicationController.class, "list", SEE_OTHER);
        }
    }
}
