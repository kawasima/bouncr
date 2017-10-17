package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.OidcProviderDao;
import net.unit8.bouncr.web.entity.OidcProvider;
import net.unit8.bouncr.web.entity.ResponseType;
import net.unit8.bouncr.web.entity.TokenEndpointAuthMethod;
import net.unit8.bouncr.web.form.OidcProviderForm;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

public class OidcProviderController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @RolesAllowed("LIST_OIDC_PROVIDERS")
    public HttpResponse list() {
        OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
        List<OidcProvider> oidcProviders = oidcProviderDao.selectAll();

        return templateEngine.render("admin/oidcProvider/list",
                "oidcProviders", oidcProviders);
    }

    @RolesAllowed("CREATE_OIDC_PROVIDER")
    public HttpResponse newForm() {
        OidcProviderForm oidcProvider = new OidcProviderForm();
        return templateEngine.render("admin/oidcProvider/new",
                "oidcProvider", oidcProvider,
                "responseTypes", ResponseType.values(),
                "tokenEndpointAuthMethods", TokenEndpointAuthMethod.values());
    }

    @Transactional
    @RolesAllowed("CREATE_OIDC_PROVIDER")
    public HttpResponse create(OidcProviderForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oidcProvider/new",
                    "oidcProvider", form,
                    "responseTypes", ResponseType.values(),
                    "tokenEndpointAuthMethods", TokenEndpointAuthMethod.values());
        } else {
            OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
            OidcProvider oidcProvider = beansConverter.createFrom(form, OidcProvider.class);
            oidcProviderDao.insert(oidcProvider);

            return UrlRewriter.redirect(OidcProviderController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed("MODIFY_OIDC_PROVIDER")
    public HttpResponse edit(Parameters params) {
        OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
        OidcProvider oidcProvider = oidcProviderDao.selectById(params.getLong("id"));
        OidcProviderForm form = beansConverter.createFrom(oidcProvider, OidcProviderForm.class);

        return templateEngine.render("admin/oidcProvider/edit",
                "oidcProvider", form,
                "responseTypes", ResponseType.values(),
                "tokenEndpointAuthMethods", TokenEndpointAuthMethod.values());
    }

    @Transactional
    @RolesAllowed("MODIFY_OIDC_PROVIDER")
    public HttpResponse update(OidcProviderForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oidcProvider/edit",
                    "oidcProvider", form,
                    "responseTypes", ResponseType.values(),
                    "tokenEndpointAuthMethods", TokenEndpointAuthMethod.values());
        } else {
            OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
            OidcProvider oidcProvider = oidcProviderDao.selectById(form.getId());
            beansConverter.copy(form, oidcProvider);
            oidcProviderDao.update(oidcProvider);

            return UrlRewriter.redirect(OidcProviderController.class, "list", SEE_OTHER);
        }
    }
}
