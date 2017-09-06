package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.GroupForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

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
        UserDao userDao = daoProvider.getDao(UserDao.class);
        List<User> users = userDao.selectAll();
        return templateEngine.render("admin/group/new",
                "group", group,
                "users", users,
                "userIds", Collections.emptyList());
    }

    @Transactional
    public HttpResponse create(GroupForm form) {
        if (form.hasErrors()) {
            UserDao userDao = daoProvider.getDao(UserDao.class);
            List<User> users = userDao.selectAll();
            return templateEngine.render("admin/group/new",
                    "group", form,
                    "users", users,
                    "userIds", Collections.emptyList());
        } else {
            GroupDao groupDao = daoProvider.getDao(GroupDao.class);
            Group group = beansConverter.createFrom(form, Group.class);
            group.setWriteProtected(false);
            groupDao.insert(group);

            UserDao userDao = daoProvider.getDao(UserDao.class);
            form.getUserId().stream().forEach(userId -> {
                User user = userDao.selectById(userId);
                groupDao.addUser(group, user);
            });

            return UrlRewriter.redirect(GroupController.class, "list", SEE_OTHER);
        }
    }

    public HttpResponse edit(Parameters params) {
        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        Group group = groupDao.selectById(params.getLong("id"));
        GroupForm form = beansConverter.createFrom(group, GroupForm.class);

        UserDao userDao = daoProvider.getDao(UserDao.class);
        List<User> users = userDao.selectAll();

        List<Long> userIds = userDao.selectByGroupId(group.getId())
                .stream()
                .map(user -> user.getId())
                .collect(Collectors.toList());

        return templateEngine.render("admin/group/edit",
                "group", form,
                "users", users,
                "userIds", userIds);
    }

    @Transactional
    public HttpResponse update(GroupForm form) {
        if (form.hasErrors()) {
            UserDao userDao = daoProvider.getDao(UserDao.class);
            List<User> users = userDao.selectAll();
            return templateEngine.render("admin/group/new",
                    "group", form,
                    "users", users);
        } else {
            GroupDao groupDao = daoProvider.getDao(GroupDao.class);
            Group group = groupDao.selectById(form.getId());
            beansConverter.copy(form, group);
            groupDao.update(group);

            UserDao userDao = daoProvider.getDao(UserDao.class);
            groupDao.clearUsers(group.getId());

            form.getUserId().stream().forEach(userId -> {
                User user = userDao.selectById(userId);
                groupDao.addUser(group, user);
            });

            return UrlRewriter.redirect(GroupController.class, "list", SEE_OTHER);
        }
    }
}
