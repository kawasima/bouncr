package net.unit8.bouncr.web.controller;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.GroupForm;
import net.unit8.bouncr.web.form.UserForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

/**
 * A controller about group actions.
 *
 * @author kawasima
 */
public class GroupController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    public HttpResponse list() {
        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        List<Group> groups = groupDao.selectAll();

        return templateEngine.render("admin/group/list",
                "groups", groups);
    }

    public HttpResponse newForm() {
        GroupForm group = new GroupForm();
        return templateEngine.render("admin/group/new",
                "group", group);
    }

    @Transactional
    public HttpResponse create(GroupForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/group/new",
                    "group", form);
        }
        return HttpResponse.of("OK");
    }

}
