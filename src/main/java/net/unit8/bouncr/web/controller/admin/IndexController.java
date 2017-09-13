package net.unit8.bouncr.web.controller.admin;

import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import kotowari.component.TemplateEngine;

import javax.inject.Inject;

/**
 * A controller for menu actions.
 *
 * @author kawasima
 */
public class IndexController {
    @Inject
    private TemplateEngine templateEngine;

    public HttpResponse home(UserPrincipal principal) {
        return templateEngine.render("admin/index/home");
    }
}
