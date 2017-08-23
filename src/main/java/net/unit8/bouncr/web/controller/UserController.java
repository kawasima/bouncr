package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.web.dao.PasswordCredentialDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.PasswordCredential;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.UserForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Random;

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

    public HttpResponse list() {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        List<User> users = userDao.selectAll();

        return templateEngine.render("admin/user/list",
                "users", users);
    }

    public HttpResponse newUser() {
        UserForm user = new UserForm();
        return templateEngine.render("admin/user/new",
                "user", user);
    }

    @Transactional
    public HttpResponse create(UserForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/user/new",
                    "user", form);
        }
        User user = beansConverter.createFrom(form, User.class);
        UserDao userDao = daoProvider.getDao(UserDao.class);
        userDao.insert(user);

        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
        Random random = new Random();
        passwordCredentialDao.insert(
                user.getId(),
                form.getPassword(),
                generateRandomString(random, 16));


        return UrlRewriter.redirect(UserController.class, "list", SEE_OTHER);
    }

    public HttpResponse edit(Parameters params) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        UserForm form = beansConverter.createFrom(user, UserForm.class);
        return templateEngine.render("admin/user/edit",
                "user", form,
                "userId", user.getId());
    }

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

        return UrlRewriter.redirect(UserController.class, "list", SEE_OTHER);
    }

    /**
     * Generate a random string.
     *
     * @param random the Random object
     * @param length the length of generated string
     * @return
     */
    private static String generateRandomString(Random random, int length){
        return random.ints(48,122)
                .filter(i-> (i<57 || i>65) && (i <90 || i>97))
                .mapToObj(i -> (char) i)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
