package net.unit8.bouncr.web.controller;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.OAuth2ProviderDao;
import net.unit8.bouncr.web.entity.OAuth2Provider;
import net.unit8.bouncr.web.form.GroupForm;
import net.unit8.bouncr.web.form.OAuth2ProviderForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

public class OAuth2ProviderController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    public HttpResponse list() {
        OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
        List<OAuth2Provider> oauth2Providers = oauth2ProviderDao.selectAll();

        return templateEngine.render("admin/oauth2Provider/list",
                "oauth2Providers", oauth2Providers);
    }

    public HttpResponse newForm() {
        OAuth2ProviderForm oauth2Provider = new OAuth2ProviderForm();
        return templateEngine.render("admin/oauth2Provider/new",
                "oauth2Provider", oauth2Provider);
    }

    @Transactional
    public HttpResponse create(GroupForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oauth2Provider/new",
                    "oauth2Provider", form);
        } else {
            OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
            OAuth2Provider oauth2Provider = beansConverter.createFrom(form, OAuth2Provider.class);
            oauth2ProviderDao.insert(oauth2Provider);

            return UrlRewriter.redirect(GroupController.class, "list", SEE_OTHER);
        }
    }

}
