package net.unit8.bouncr;

import enkan.Application;
import enkan.Endpoint;
import enkan.Env;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.endpoint.ResourceEndpoint;
import enkan.middleware.*;
import enkan.middleware.devel.HttpStatusCatMiddleware;
import enkan.middleware.devel.StacktraceMiddleware;
import enkan.middleware.devel.TraceWebMiddleware;
import enkan.middleware.doma2.DomaTransactionMiddleware;
import enkan.middleware.metrics.MetricsMiddleware;
import enkan.middleware.session.JCacheStore;
import enkan.middleware.session.KeyValueStore;
import enkan.middleware.session.MemoryStore;
import enkan.predicate.PathPredicate;
import enkan.security.backend.SessionBackend;
import enkan.system.inject.ComponentInjector;
import enkan.util.HttpResponseUtils;
import kotowari.middleware.*;
import kotowari.middleware.serdes.ToStringBodyWriter;
import kotowari.routing.Routes;
import net.unit8.bouncr.authn.BouncrStoreBackend;
import net.unit8.bouncr.web.controller.*;
import net.unit8.bouncr.web.entity.Permission;
import net.unit8.bouncr.web.entity.Role;

import java.util.Collections;
import java.util.Objects;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.Predicates.*;

/**
 * The factory for Bouncr application.
 *
 * @author kawasima
 */
public class BouncrApplicationFactory implements ApplicationFactory {
    @Override
    public Application create(ComponentInjector injector) {
        WebApplication app = new WebApplication();

        // Routing
        Routes routes = Routes.define(r -> {
            r.get("/admin/").to(IndexController.class, "home");

            /* Routing for user actions */
            r.get("/admin/user").to(UserController.class, "list");
            r.get("/admin/user/new").to(UserController.class, "newUser");
            r.post("/admin/user").to(UserController.class, "create");
            r.get("/admin/user/:id/edit").to(UserController.class, "edit");
            r.post("/admin/user/:id").to(UserController.class, "update");
            r.post("/admin/user/:id/delete").to(UserController.class, "delete");

            /* Routing for group actions */
            r.get("/admin/group").to(GroupController.class, "list");

            /* Routing for application actions */
            r.get("/admin/application").to(ApplicationController.class, "list");

            /* Routing for permission actions */
            r.get("/admin/permission").to(PermissionController.class, "list");

            /* Routing for role actions */
            r.get("/admin/role").to(RoleController.class, "list");

            /* My page */
            r.get("/my/login").to(LoginController.class, "loginForm");
            r.post("/my/login").to(LoginController.class, "loginByPassword");
            r.get("/my").to(MyController.class, "home");
        }).compile();

        // Enkan
        app.use(new DefaultCharsetMiddleware());
        app.use(new MetricsMiddleware<>());
        app.use(NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(envIn("development"), new StacktraceMiddleware());
        app.use(envIn("development"), new TraceWebMiddleware());
        app.use(new TraceMiddleware<>());
        app.use(new ContentTypeMiddleware());
        app.use(envIn("development"), new HttpStatusCatMiddleware());
        app.use(new ParamsMiddleware());
        app.use(new MultipartParamsMiddleware());
        app.use(new MethodOverrideMiddleware());
        app.use(new NormalizationMiddleware());
        app.use(new NestedParamsMiddleware());
        app.use(new CookiesMiddleware());

        app.use(new AuthenticationMiddleware<>(Collections.singletonList(injector.inject(new BouncrStoreBackend()))));
        app.use(and(path("^(/my(?!/login)|/admin)"), authenticated().negate()),
                (Endpoint<HttpRequest, HttpResponse>) req ->
                        HttpResponseUtils.redirect("/my/login?url=" + req.getUri(),
                                HttpResponseUtils.RedirectStatusCode.TEMPORARY_REDIRECT));

        app.use(new ContentNegotiationMiddleware());
        // Kotowari
        app.use(new ResourceMiddleware());
        app.use(new RenderTemplateMiddleware());
        app.use(new RoutingMiddleware(routes));
        app.use(new DomaTransactionMiddleware<>());
        app.use(new FormMiddleware());
        app.use(builder(new SerDesMiddleware())
                .set(SerDesMiddleware::setBodyWriters, new ToStringBodyWriter())
                .build());
        app.use(new ValidateFormMiddleware());
        app.use(new ControllerInvokerMiddleware(injector));

        return app;
    }
}
