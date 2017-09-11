package net.unit8.bouncr;

import enkan.Application;
import enkan.Endpoint;
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
import enkan.system.inject.ComponentInjector;
import enkan.util.HttpResponseUtils;
import kotowari.middleware.*;
import kotowari.middleware.serdes.ToStringBodyWriter;
import kotowari.routing.Routes;
import net.unit8.bouncr.authn.BouncrStoreBackend;
import net.unit8.bouncr.authz.AuthorizeControllerMethodMiddleware;
import net.unit8.bouncr.i18n.I18nMiddleware;
import net.unit8.bouncr.web.controller.*;
import net.unit8.bouncr.web.controller.admin.*;
import net.unit8.bouncr.web.controller.api.OAuth2Controller;

import java.util.Collections;

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
            r.scope("/admin", ar -> {
                ar.get("/").to(IndexController.class, "home");
                /* Routing for user actions */
                ar.get("/user").to(UserController.class, "list");
                ar.get("/user/new").to(UserController.class, "newUser");
                ar.post("/user").to(UserController.class, "create");
                ar.get("/user/:id/edit").to(UserController.class, "edit");
                ar.post("/user/:id").to(UserController.class, "update");
                ar.post("/user/:id/delete").to(UserController.class, "delete");

                /* Routing for group actions */
                ar.get("/group").to(GroupController.class, "list");
                ar.get("/group/new").to(GroupController.class, "newForm");
                ar.post("/group").to(GroupController.class, "create");
                ar.get("/group/:id/edit").to(GroupController.class, "edit");
                ar.post("/group/:id").to(GroupController.class, "update");
                ar.post("/group/:id/delete").to(GroupController.class, "delete");

                /* Routing for application actions */
                ar.get("/application").to(ApplicationController.class, "list");
                ar.get("/application/new").to(ApplicationController.class, "newForm");
                ar.post("/application").to(ApplicationController.class, "create");
                ar.get("/application/:id/edit").to(ApplicationController.class, "edit");
                ar.post("/application/:id").to(ApplicationController.class, "update");
                ar.post("/application/:id/delete").to(ApplicationController.class, "delete");
                ar.get("/application/:applicationId/realms").to(RealmController.class, "listByApplicationId");
                /* Routing for realm actions */
                ar.get("/application/:applicationId/realms/new").to(RealmController.class, "newForm");
                ar.post("/application/:applicationId/realms").to(RealmController.class, "create");
                ar.get("/application/:applicationId/realms/:id/edit").to(RealmController.class, "edit");
                ar.post("/application/:applicationId/realms/:id").to(RealmController.class, "update");
                ar.post("/application/:applicationId/realms/:id/delete").to(RealmController.class, "delete");

                /* Routing for permission actions */
                ar.get("/permission").to(PermissionController.class, "list");
                ar.get("/permission/new").to(PermissionController.class, "newForm");
                ar.post("/permission").to(PermissionController.class, "create");
                ar.get("/permission/:id/edit").to(PermissionController.class, "edit");
                ar.post("/permission/:id").to(PermissionController.class, "update");
                ar.post("/permission/:id/delete").to(PermissionController.class, "delete");

                /* Routing for role actions */
                ar.get("/role").to(RoleController.class, "list");
                ar.get("/role/new").to(RoleController.class, "newForm");
                ar.post("/role").to(RoleController.class, "create");
                ar.get("/role/:id/edit").to(RoleController.class, "edit");
                ar.post("/role/:id").to(RoleController.class, "update");
                ar.post("/role/:id/delete").to(RoleController.class, "delete");

                /* Routing for oauth2 application actions */
                ar.get("/oauth2app").to(OAuth2ApplicationController.class, "list");
                ar.get("/oauth2app/new").to(OAuth2ApplicationController.class, "newForm");
                ar.post("/oauth2app").to(OAuth2ApplicationController.class, "create");
                ar.get("/oauth2app/:id/edit").to(OAuth2ApplicationController.class, "edit");
                ar.post("/oauth2app/:id").to(OAuth2ApplicationController.class, "update");
                ar.post("/oauth2app/:id/delete").to(OAuth2ApplicationController.class, "delete");
            });

            /* My page */
            r.get("/my/signIn").to(SignInController.class, "signInForm");
            r.post("/my/signIn").to(SignInController.class, "signInByPassword");
            r.post("/my/signIn/clientDN").to(SignInController.class, "signInByClientDN");
            r.get("/my/signIn/oauth").to(SignInController.class, "signInByOAuth");
            r.post("/my/signOut").to(SignInController.class, "logout");
            r.get("/my/account").to(MyController.class, "account");
            r.post("/my/account").to(MyController.class, "changePassword");
            r.get("/my").to(MyController.class, "home");

            /* OAuth */
            r.get("/my/oauth/authorize").to(OAuth2Controller.class, "authorize");
            r.post("/my/oauth/accessToken").to(OAuth2Controller.class, "accessToken");
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
        app.use(and(path("^(/my(?!(/signIn|/assets))|/admin)($|/.*)"), authenticated().negate()),
                (Endpoint<HttpRequest, HttpResponse>) req ->
                        HttpResponseUtils.redirect("/my/signIn?url=" + req.getUri(),
                                HttpResponseUtils.RedirectStatusCode.TEMPORARY_REDIRECT));

        app.use(new ContentNegotiationMiddleware());
        // Kotowari
        app.use(new ResourceMiddleware());
        app.use(new RenderTemplateMiddleware());
        app.use(new I18nMiddleware());
        app.use(new RoutingMiddleware(routes));
        app.use(new AuthorizeControllerMethodMiddleware());
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
