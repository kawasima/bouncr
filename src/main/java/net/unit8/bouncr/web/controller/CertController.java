package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import enkan.exception.UnreachableException;
import enkan.security.UserPrincipal;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.cert.CertificateProvider;
import net.unit8.bouncr.cert.ReloadableTrustManager;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.CertDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Cert;
import net.unit8.bouncr.web.entity.User;

import javax.inject.Inject;
import javax.security.auth.x500.X500PrivateCredential;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.time.ZoneId;
import java.util.List;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.*;

public class CertController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private CertificateProvider certificateProvider;

    @Inject
    private ReloadableTrustManager trustManager;

    public HttpResponse list(UserPermissionPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        CertDao certDao = daoProvider.getDao(CertDao.class);

        User user = userDao.selectById(principal.getId());
        List<Cert> certs = certDao.selectByUserId(user.getId());
        return templateEngine.render("my/cert",
                "user", user,
                "certs", certs);
    }

    @Transactional
    public HttpResponse create(UserPermissionPrincipal principal, Parameters params) {
        try {
            X500PrivateCredential credential = certificateProvider.generateClientCertificate(principal.getName());
            CertDao certDao = daoProvider.getDao(CertDao.class);
            Cert cert = builder(new Cert())
                    .set(Cert::setUserId, principal.getId())
                    .set(Cert::setSerial, credential.getCertificate().getSerialNumber())
                    .set(Cert::setExpires, credential.getCertificate().getNotAfter()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                    .build();
            certDao.insert(cert);
            String p12Password = params.get("password");
            // TODO should execute asynchronously
            trustManager.addEntry(credential.getCertificate());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null, null);
            keystore.setKeyEntry(credential.getCertificate().getSubjectX500Principal().getName(),
                    credential.getPrivateKey(),
                    p12Password.toCharArray(),
                    new Certificate[]{ credential.getCertificate() });
            keystore.store(baos, p12Password.toCharArray());
            return HttpResponse.of(new ByteArrayInputStream(baos.toByteArray()));
        } catch (CertificateException e) {
            // TODO maybe incorrect
            throw new UnreachableException(e);
        } catch (KeyStoreException e) {
            throw new UnreachableException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new UnreachableException(e);
        } catch (IOException e) {
            throw new UnreachableException(e);
        }
    }
}
