package net.unit8.bouncr.web.controller;

import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.web.form.UserForm;

import javax.inject.Inject;

/**
 * @author kawasima
 */
public class GroupController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    public HttpResponse newUser() {
        UserForm user = new UserForm();
        return templateEngine.render("user/new",
                "user", user);
    }

    public HttpResponse create(UserForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("user/new",
                    "user", form);
        }
        return HttpResponse.of("OK");
    }
}
