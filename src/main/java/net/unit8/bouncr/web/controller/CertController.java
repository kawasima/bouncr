package net.unit8.bouncr.web.controller;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.cert.X509CertificateUtils;
import net.unit8.bouncr.web.dao.CertDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Cert;
import net.unit8.bouncr.web.entity.User;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.*;

public class CertController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    public HttpResponse list(UserPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        CertDao certDao = daoProvider.getDao(CertDao.class);

        User user = userDao.selectByAccount(principal.getName());
        List<Cert> certs = certDao.selectByUserId(user.getId());
        return templateEngine.render("my/cert",
                "user", user,
                "certs", certs);
    }

    @Transactional
    public HttpResponse create(UserPrincipal principal) {
        //X509CertificateUtils.generateClientCertificate();
        return UrlRewriter.redirect(CertController.class, "list", SEE_OTHER);
    }
}
