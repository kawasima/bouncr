package net.unit8.bouncr.extention.app;

import enkan.Application;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.system.inject.ComponentInjector;
import kotowari.routing.Routes;
import net.unit8.bouncr.extention.app.resource.VerificationEmailResource;

public class EmailApplicationFactory implements ApplicationFactory {
    @Override
    public Application create(ComponentInjector injector) {
        WebApplication app = new WebApplication();

        // Routing
        Routes routes = Routes.define(r -> {
            r.scope("/bouncr/api", ar -> {
                ar.all("/verification/email").to(VerificationEmailResource.class);
            });
        }).compile();

        return app;
    }
}
