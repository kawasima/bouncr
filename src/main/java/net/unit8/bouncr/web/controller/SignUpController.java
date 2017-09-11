package net.unit8.bouncr.web.controller;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.SignUpForm;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class SignUpController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    public HttpResponse newForm() {
        return templateEngine.render("my/signUp/new.ftl",
                "signup", new SignUpForm());
    }

    @Transactional
    public HttpResponse create(SignUpForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("my/signUp/new.ftl",
                    "signup", form);
        } else {
            User user = beansConverter.createFrom(form, User.class);
            UserDao userDao = daoProvider.getDao(UserDao.class);
            userDao.insert(user);
            return templateEngine.render("my/signUp/complete.ftl");
        }
    }
}
