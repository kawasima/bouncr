package net.unit8.bouncr.web.controller;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.entity.Permission;

import javax.inject.Inject;
import java.util.List;

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

    public HttpResponse list() {
        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        List<Permission> permissions =permissionDao.selectAll();

        return templateEngine.render("admin/permission/list",
                "permissions", permissions);
    }
}
