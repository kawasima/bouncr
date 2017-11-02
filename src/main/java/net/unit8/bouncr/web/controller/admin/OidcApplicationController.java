package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.OidcApplicationDao;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.entity.OidcApplication;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.form.OidcApplicationForm;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.seasar.doma.jdbc.SelectOptions;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.ThreadingUtils.some;

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
        OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        List<OidcApplication> oidcApplications = oidcApplicationDao.selectAll();

        return templateEngine.render("admin/oidcApplication/list",
                "oidcApplications", oidcApplications);
    }

    @RolesAllowed({"LIST_OIDC_APPLICATIONS"})
    public HttpResponse show(Parameters params) {
        OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        OidcApplication oidcApplication = oidcApplicationDao.selectById(params.getLong("id"));

        return templateEngine.render("admin/oidcApplication/show",
                "oidcApplication", oidcApplication);
    }

    private HttpResponse responseNewForm(OidcApplicationForm form, UserPermissionPrincipal principal) {
        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        List<Permission> permissions = permissionDao.selectByPrincipalScope(principal, SelectOptions.get());
        return templateEngine.render("admin/oidcApplication/new",
                "oidcApplication", form,
                "permissions", permissions);
    }

    @RolesAllowed("CREATE_OIDC_APPLICATION")
    public HttpResponse newForm(UserPermissionPrincipal principal) {
        return responseNewForm(new OidcApplicationForm(), principal);
    }

    @RolesAllowed("MODIFY_OIDC_APPLICATION")
    public HttpResponse edit(Parameters params, UserPermissionPrincipal principal) {
        OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        OidcApplication oidcApplication = oidcApplicationDao.selectById(params.getLong("id"));
        OidcApplicationForm form = beansConverter.createFrom(oidcApplication, OidcApplicationForm.class);
        form.setPermissionId(oidcApplicationDao.selectPermissionsById(oidcApplication.getId())
                .stream()
                .map(p -> p.getId())
                .collect(Collectors.toList()));

        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        List<Permission> permissions = permissionDao.selectByPrincipalScope(principal, SelectOptions.get());
        return templateEngine.render("admin/oidcApplication/edit",
                "oidcApplication", form,
                "permissions", permissions);
    }

    @RolesAllowed("CREATE_OIDC_APPLICATION")
    @Transactional
    public HttpResponse create(OidcApplicationForm form, UserPermissionPrincipal principal) {
        if (form.hasErrors()) {
            return responseNewForm(form, principal);
        } else {
            OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
            OidcApplication oidcApplication = beansConverter.createFrom(form, OidcApplication.class);
            oidcApplication.setClientId(RandomUtils.generateRandomString(16, config.getSecureRandom()));
            oidcApplication.setClientSecret(RandomUtils.generateRandomString(32, config.getSecureRandom()));

            KeyPair keyPair = KeyUtils.generate(2048, config.getSecureRandom());
            oidcApplication.setPublicKey(keyPair.getPublic().getEncoded());
            oidcApplication.setPrivateKey(keyPair.getPrivate().getEncoded());

            oidcApplicationDao.insert(oidcApplication);

            oidcApplicationDao.clearScopes(oidcApplication);
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            some(form.getPermissionId(),
                    pids ->pids.stream()
                            .map(permissionDao::selectById)
                            .map(p -> oidcApplicationDao.addScope(oidcApplication, p))
                            .collect(Collectors.toList()));

            return UrlRewriter.redirect(OidcApplicationController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed("MODIFY_OIDC_APPLICATION")
    @Transactional
    public HttpResponse update(OidcApplicationForm form, UserPermissionPrincipal principal) {
        if (form.hasErrors()) {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            List<Permission> permissions = permissionDao.selectByPrincipalScope(principal, SelectOptions.get());

            return templateEngine.render("admin/oidcApplication/edit",
                    "OidcApplication", form,
                    "permissions", permissions);
        } else {
            OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
            OidcApplication oidcApplication = oidcApplicationDao.selectById(form.getId());
            beansConverter.copy(form, oidcApplication);
            oidcApplicationDao.update(oidcApplication);

            oidcApplicationDao.clearScopes(oidcApplication);
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            some(form.getPermissionId(),
                    pids ->pids.stream()
                            .map(permissionDao::selectById)
                            .map(p -> oidcApplicationDao.addScope(oidcApplication, p))
                            .collect(Collectors.toList()));

            return UrlRewriter.redirect(OidcApplicationController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed("DELETE_OIDC_APPLICATION")
    @Transactional
    public HttpResponse delete(OidcApplicationForm form) {
        OidcApplicationDao oidcApplicationDao = daoProvider.getDao(OidcApplicationDao.class);
        OidcApplication oidcApplication = oidcApplicationDao.selectById(form.getId());
        oidcApplicationDao.delete(oidcApplication);
        return UrlRewriter.redirect(OidcApplicationController.class, "list", SEE_OTHER);
    }
}
