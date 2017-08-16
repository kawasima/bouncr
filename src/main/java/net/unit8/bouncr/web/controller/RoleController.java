package net.unit8.bouncr.web.controller;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.dao.RoleDao;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.entity.Role;

import javax.inject.Inject;
import java.util.List;

/**
 * A controller for role actions.
 *
 * @author kawasima
 */
public class RoleController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    public HttpResponse list() {
        RoleDao roleDao= daoProvider.getDao(RoleDao.class);
        List<Role> roles =roleDao.selectAll();

        return templateEngine.render("admin/role/list",
                "roles", roles);
    }
}
