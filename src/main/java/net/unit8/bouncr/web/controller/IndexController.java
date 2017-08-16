package net.unit8.bouncr.web.controller;

import enkan.data.HttpResponse;
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

    public HttpResponse home() {
        return templateEngine.render("admin/index/home");
    }
}
