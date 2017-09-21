package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.OidcApplicationDao;
import net.unit8.bouncr.web.entity.OidcApplication;
import net.unit8.bouncr.web.form.OidcApplicationForm;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.List;
import java.util.Random;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

public class OidcApplicationController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private BouncrConfiguration config;

    @RolesAllowed({"LIST_OIDC_APPLICATIONS"})
    public HttpResponse list() {
        OidcApplicationDao OidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        List<OidcApplication> oidcApplications = OidcApplicationDao.selectAll();

        return templateEngine.render("admin/oidcApplication/list",
                "oidcApplications", oidcApplications);
    }

    @RolesAllowed("CREATE_OIDC_APPLICATION")
    public HttpResponse newForm() {
        return templateEngine.render("admin/oidcApplication/new",
                "oidcApplication", new OidcApplicationForm());
    }

    @RolesAllowed("MODIFY_OIDC_APPLICATION")
    public HttpResponse edit(Parameters params) {
        OidcApplicationDao OidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        OidcApplication OidcApplication = OidcApplicationDao.selectById(params.getLong("id"));
        OidcApplicationForm form = beansConverter.createFrom(OidcApplication, OidcApplicationForm.class);
        return templateEngine.render("admin/oidcApplication/edit",
                "oidcApplication", form);
    }

    @RolesAllowed("CREATE_OIDC_APPLICATION")
    @Transactional
    public HttpResponse create(OidcApplicationForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oidcApplication/new",
                    "oidcApplication", form);
        } else {
            OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
            OidcApplication oidcApplication = beansConverter.createFrom(form, OidcApplication.class);
            oidcApplication.setClientId(RandomUtils.generateRandomString(16, config.getSecureRandom()));
            oidcApplication.setClientSecret(RandomUtils.generateRandomString(32, config.getSecureRandom()));

            KeyPair keyPair = KeyUtils.generate(2048, config.getSecureRandom());
            oidcApplication.setPublicKey(keyPair.getPublic().getEncoded());
            oidcApplication.setPrivateKey(keyPair.getPrivate().getEncoded());

            oidcApplicationDao.insert(oidcApplication);
            return UrlRewriter.redirect(OidcApplicationController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed("MODIFY_OIDC_APPLICATION")
    @Transactional
    public HttpResponse update(OidcApplicationForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/oidcApplication/edit",
                    "OidcApplication", form);
        } else {
            OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
            OidcApplication oidcApplication = oidcApplicationDao.selectById(form.getId());
            beansConverter.copy(form, oidcApplication);
            oidcApplicationDao.update(oidcApplication);
            return UrlRewriter.redirect(OidcApplicationController.class, "list", SEE_OTHER);
        }
    }
}
