package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.dao.RoleDao;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.entity.Role;
import net.unit8.bouncr.web.form.PermissionForm;
import net.unit8.bouncr.web.form.RoleForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

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
    public HttpResponse newForm(RoleForm form) {
        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        List<Permission> permissions = permissionDao.selectAll();
        return templateEngine.render("admin/role/new",
                "role", form,
                "permissions", permissions);
    }

    public HttpResponse edit(Parameters params) {
        RoleDao roleDao = daoProvider.getDao(RoleDao.class);
        Role role = roleDao.selectById(params.getLong("id"));
        RoleForm form = beansConverter.createFrom(role, RoleForm.class);

        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        List<Permission> permissions = permissionDao.selectAll();

        List<Long> rolePermissionIds = permissionDao.selectByRoleId(role.getId())
                .stream()
                .map(Permission::getId)
                .collect(Collectors.toList());

        return templateEngine.render("admin/role/edit",
                "role", form,
                "permissions", permissions,
                "rolePermissionIds", rolePermissionIds);
    }

    @Transactional
    public HttpResponse create(RoleForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/role/new",
                    "role", form);
        } else {
            RoleDao roleDao = daoProvider.getDao(RoleDao.class);
            Role role = beansConverter.createFrom(form, Role.class);
            role.setWriteProtected(false);
            roleDao.insert(role);

            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            form.getPermissionId().stream()
                    .map(permissionId -> permissionDao.selectById(permissionId))
                    .forEach(p -> roleDao.addPermission(role, p));
            return UrlRewriter.redirect(RoleController.class, "list", SEE_OTHER);
        }
    }

    @Transactional
    public HttpResponse update(RoleForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/role/edit",
                    "role", form);
        } else {
            RoleDao roleDao = daoProvider.getDao(RoleDao.class);
            Role role = roleDao.selectById(form.getId());
            beansConverter.copy(form, role);
            roleDao.update(role);

            roleDao.clearPermissions(role);
            PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
            form.getPermissionId().stream()
                    .map(permissionId -> permissionDao.selectById(permissionId))
                    .forEach(p -> roleDao.addPermission(role, p));

            return UrlRewriter.redirect(RoleController.class, "list", SEE_OTHER);
        }
    }

}
