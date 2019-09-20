package net.unit8.bouncr.extention.app;

import enkan.MiddlewareChain;
import enkan.application.WebApplication;
import kotowari.middleware.RoutingMiddleware;
import kotowari.routing.Routes;
import net.unit8.bouncr.extention.app.resource.VerificationEmailResource;

import java.util.function.Function;

/**
 * A customizer for email application.
 *
 * @author kawasima
 */
public class EmailApplicationCustomizer implements Function<WebApplication, WebApplication> {
    @Override
    public WebApplication apply(WebApplication app) {
        Routes routes = Routes.define(r -> {
            r.scope("/bouncr/api", ar -> {
                ar.all("/verification/:account/email").to(VerificationEmailResource.class);
            });
        }).compile();

        app.getMiddlewareStack().stream()
                .map(MiddlewareChain::getMiddleware)
                .filter(RoutingMiddleware.class::isInstance)
                .map(RoutingMiddleware.class::cast)
                .forEach(routing -> routing.getRoutes().concat(routes));
        return app;
    }
}
