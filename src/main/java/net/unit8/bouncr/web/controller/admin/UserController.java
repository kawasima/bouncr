package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.PasswordCredentialDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.PasswordCredential;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.UserForm;
import org.seasar.doma.jdbc.SelectOptions;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Random;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

/**
 * A controller for user actions.
 *
 * @author kawasima
 */
public class UserController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private BouncrConfiguration config;

    @RolesAllowed({"LIST_USERS", "LIST_ANY_USERS"})
    public HttpResponse list(UserPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);

        SelectOptions options = SelectOptions.get();
        List<User> users = userDao.selectByPrincipalScope(principal, options);

        return templateEngine.render("admin/user/list",
                "users", users);
    }

    @RolesAllowed({"LIST_USERS", "LIST_ANY_USERS"})
    public HttpResponse show(UserPrincipal principal, Parameters params) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        boolean isLock = userDao.isLock(user.getAccount());

        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        List<Group> groups = groupDao.selectByUserId(user.getId());

        return templateEngine.render("admin/user/show",
                "user", user,
                "groups", groups,
                "isLock", isLock);
    }

    @RolesAllowed({"LIST_USERS", "LIST_ANY_USERS"})
    public List<User> search(Parameters params, UserPrincipal principal) {
        String word = params.get("q");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        SelectOptions options = SelectOptions.get();
        return userDao.selectForIncrementalSearch(word + "%", principal, options);
    }

    @RolesAllowed("CREATE_USER")
    public HttpResponse newUser() {
        UserForm user = new UserForm();
        return templateEngine.render("admin/user/new",
                "user", user);
    }

    @RolesAllowed("CREATE_USER")
    @Transactional
    public HttpResponse create(UserForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/user/new",
                    "user", form);
        }
        User user = beansConverter.createFrom(form, User.class);
        user.setWriteProtected(false);
        UserDao userDao = daoProvider.getDao(UserDao.class);
        userDao.insert(user);

        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredentialDao.insert(builder(new PasswordCredential())
                .set(PasswordCredential::setId, user.getId())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(form.getPassword(), salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .build());

        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        Group bouncrUserGroup = groupDao.selectByName("BOUNCR_USER");
        groupDao.addUser(bouncrUserGroup, user);

        return UrlRewriter.redirect(UserController.class, "list", SEE_OTHER);
    }

    @RolesAllowed({"MODIFY_USER", "MODIFY_ANY_USER"})
    public HttpResponse edit(Parameters params) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        UserForm form = beansConverter.createFrom(user, UserForm.class);
        return templateEngine.render("admin/user/edit",
                "user", form,
                "userId", user.getId());
    }

    @RolesAllowed({"MODIFY_USER", "MODIFY_ANY_USER"})
    @Transactional
    public HttpResponse update(UserForm form, Parameters params) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/user/edit",
                    "user", form);
        }
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        beansConverter.copy(form, user);
        userDao.update(user);

        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredentialDao.insert(builder(new PasswordCredential())
                .set(PasswordCredential::setId, user.getId())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(form.getPassword(), salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .build());

        return UrlRewriter.redirect(UserController.class, "list", SEE_OTHER);
    }

    @RolesAllowed({"UNLOCK_USER", "UNLOCK_ANY_USER"})
    @Transactional
    public HttpResponse unlock(Parameters params) {
        Long id = params.getLong("id");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(id);
        userDao.unlock(user.getId());
        return UrlRewriter.redirect(UserController.class, "show?id=" + id, SEE_OTHER);
    }

    @RolesAllowed({"LOCK_USER", "LOCK_ANY_USER"})
    @Transactional
    public HttpResponse lock(Parameters params) {
        Long id = params.getLong("id");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(id);
        userDao.lock(user.getId());
        return UrlRewriter.redirect(UserController.class, "show?id=" + id, SEE_OTHER);
    }

}
