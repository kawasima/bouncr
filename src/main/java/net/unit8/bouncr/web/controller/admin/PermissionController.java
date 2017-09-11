package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.form.PermissionForm;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

/**
 * A controller for permission actions.
 *
 * @author kawasima
 */
public class PermissionController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @RolesAllowed("LIST_PERMISSIONS")
    public HttpResponse list() {
        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        List<Permission> permissions =permissionDao.selectAll();

        return templateEngine.render("admin/permission/list",
                "permissions", permissions);
    }

    @RolesAllowed("CREATE_PERMISSION")
    public HttpResponse newForm(PermissionForm form) {
        return templateEngine.render("admin/permission/new",
                "permission", form);
    }

    @RolesAllowed("MODIFY_PERMISSION")
    public HttpResponse edit(Parameters params) {
        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        Permission permission = permissionDao.selectById(params.getLong("id"));
        PermissionForm form = beansConverter.createFrom(permission, PermissionForm.class);
        return templateEngine.render("admin/permission/edit",
                "permission", form);
    }

    @RolesAllowed("CREATE_PERMISSION")
    @Transactional
    public HttpResponse create(PermissionForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/permission/new",
                    "permission", form);
        } else {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            Permission permission = beansConverter.createFrom(form, Permission.class);
            permissionDao.insert(permission);
            return UrlRewriter.redirect(PermissionController.class, "list", SEE_OTHER);
        }
    }

    @Transactional
    @RolesAllowed("MODIFY_PERMISSION")
    public HttpResponse update(PermissionForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/permission/edit",
                    "permission", form);
        } else {
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            Permission permission = permissionDao.selectById(form.getId());
            beansConverter.copy(form, permission);
            permissionDao.update(permission);
            return UrlRewriter.redirect(PermissionController.class, "list", SEE_OTHER);
        }
    }
}
