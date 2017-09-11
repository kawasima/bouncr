package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.OAuth2ProviderDao;
import net.unit8.bouncr.web.entity.OAuth2Provider;
import net.unit8.bouncr.web.form.OAuth2ProviderForm;

import javax.annotation.security.RolesAllowed;
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

    @RolesAllowed("LIST_OAUTH2_PROVIDER")
    public HttpResponse list() {
        OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
        List<OAuth2Provider> oauth2Providers = oauth2ProviderDao.selectAll();

        return templateEngine.render("admin/oauth2Provider/list",
                "oauth2Providers", oauth2Providers);
    }

    @RolesAllowed("CREATE_OAUTH2_PROVIDER")
    public HttpResponse newForm() {
        OAuth2ProviderForm oauth2Provider = new OAuth2ProviderForm();
        return templateEngine.render("admin/oauth2Provider/new",
                "oauth2Provider", oauth2Provider);
    }

    @Transactional
    @RolesAllowed("CREATE_OAUTH2_PROVIDER")
    public HttpResponse create(OAuth2ProviderForm form) {
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

    @RolesAllowed("MODIFY_OAUTH2_PROVIDER")
    public HttpResponse edit(Parameters params) {
        OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
        OAuth2Provider oauth2Provider = oauth2ProviderDao.selectById(params.getLong("id"));
        OAuth2ProviderForm form = beansConverter.createFrom(oauth2Provider, OAuth2ProviderForm.class);

        return templateEngine.render("admin/oauth2Provider/edit",
                "oauth2Provider", form);
    }

    @Transactional
    @RolesAllowed("MODIFY_OAUTH2_PROVIDER")
    public HttpResponse update(OAuth2ProviderForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oauth2Provider/edit",
                    "oauth2Provider", form);
        } else {
            OAuth2ProviderDao oauth2ProviderDao = daoProvider.getDao(OAuth2ProviderDao.class);
            OAuth2Provider oauth2Provider = oauth2ProviderDao.selectById(form.getId());
            beansConverter.copy(form, oauth2Provider);

            return UrlRewriter.redirect(GroupController.class, "list", SEE_OTHER);
        }
    }
}
