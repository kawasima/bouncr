package net.unit8.bouncr.web.controller.admin;

import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;

import javax.inject.Inject;

public class AuditController {
    @Inject
    private DomaProvider daoProvider;

    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private BeansConverter beansConverter;

    public HttpResponse showLoginLogs() {
        return templateEngine.render("admin/audit/list");
    }
}
