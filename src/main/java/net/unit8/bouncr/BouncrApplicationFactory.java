package net.unit8.bouncr;

import enkan.Application;
import enkan.Endpoint;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.data.ContentNegotiable;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.endpoint.ResourceEndpoint;
import enkan.exception.MisconfigurationException;
import enkan.middleware.*;
import enkan.middleware.doma2.DomaTransactionMiddleware;
import enkan.middleware.metrics.MetricsMiddleware;
import enkan.system.inject.ComponentInjector;
import enkan.util.HttpResponseUtils;
import is.tagomor.woothee.Classifier;
import kotowari.middleware.*;
import kotowari.middleware.serdes.ToStringBodyWriter;
import kotowari.routing.Routes;
import net.unit8.bouncr.authn.BouncrStoreBackend;
import net.unit8.bouncr.authz.AuthorizeControllerMethodMiddleware;
import net.unit8.bouncr.i18n.I18nMiddleware;
import net.unit8.bouncr.util.DigestUtils;
import net.unit8.bouncr.web.controller.MyController;
import net.unit8.bouncr.web.controller.SignInController;
import net.unit8.bouncr.web.controller.SignUpController;
import net.unit8.bouncr.web.controller.admin.*;
import net.unit8.bouncr.web.controller.api.OidcController;
import net.unit8.bouncr.web.controller.api.GroupApiController;

import java.util.*;
import java.util.function.Function;

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
                ar.get("/user/:id").to(UserController.class, "show");
                ar.get("/user/:id/edit").to(UserController.class, "edit");
                ar.post("/user/:id/lock").to(UserController.class, "lock");
                ar.post("/user/:id/unlock").to(UserController.class, "unlock");
                ar.post("/user/:id").to(UserController.class, "update");
                ar.post("/user/:id/delete").to(UserController.class, "delete");

                /* Routing for group actions */
                ar.get("/group").to(GroupController.class, "list");
                ar.get("/group/new").to(GroupController.class, "newForm");
                ar.post("/group").to(GroupController.class, "create");
                ar.get("/group/:id").to(GroupController.class, "show");
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

                ar.get("/oidcProvider").to(OidcProviderController.class, "list");
                ar.get("/oidcProvider/new").to(OidcProviderController.class, "newForm");
                ar.post("/oidcProvider").to(OidcProviderController.class, "create");
                ar.get("/oidcProvider/:id/edit").to(OidcProviderController.class, "edit");
                ar.post("/oidcProvider/:id").to(OidcProviderController.class, "update");
                ar.post("/oidcProvider/:id/delete").to(OidcProviderController.class, "delete");

                /* Routing for oidc application actions */
                ar.get("/oidcApp").to(OidcApplicationController.class, "list");
                ar.get("/oidcApp/new").to(OidcApplicationController.class, "newForm");
                ar.post("/oidcApp").to(OidcApplicationController.class, "create");
                ar.get("/oidcApp/:id/edit").to(OidcApplicationController.class, "edit");
                ar.post("/oidcApp/:id").to(OidcApplicationController.class, "update");
                ar.post("/oidcApp/:id/delete").to(OidcApplicationController.class, "delete");

                ar.get("/userProfile").to(UserProfileController.class, "list");
                ar.get("/userProfile/new").to(UserProfileController.class, "newForm");
                ar.post("/userProfile").to(UserProfileController.class, "create");
                ar.get("/userProfile/:id/edit").to(UserProfileController.class, "edit");
                ar.post("/userProfile/:id").to(UserProfileController.class, "update");

                ar.get("/invitation/new").to(InvitationController.class, "newForm");
                ar.post("/invitation/").to(InvitationController.class, "create");

                /* Admin api */
                ar.scope("/api", api -> {
                    /* Routing for group apis */
                    api.post("/group/:id/users").to(net.unit8.bouncr.web.controller.api.admin.GroupController.class, "addUser");
                    api.get("/group/:id/users").to(GroupApiController.class, "users");
                    api.get("user/search").to(UserController.class, "search");
                });
            });

            /* My page */
            r.scope("/my", mr-> {
                mr.get("/signIn").to(SignInController.class, "signInForm");
                mr.post("/signIn").to(SignInController.class, "signInByPassword");
                mr.post("/signIn/changePassword").to(SignInController.class, "forceToChangePassword");
                mr.post("/signIn/clientDN").to(SignInController.class, "signInByClientDN");
                mr.get("/signIn/oidc/:id").to(SignInController.class, "signInByOidc");
                mr.post("/signIn/oidc/:id").to(SignInController.class, "signInByOidcImplicit");
                mr.post("/signOut").to(SignInController.class, "signOut");


                mr.get("/signUp").to(SignUpController.class, "newForm");
                mr.post("/signUp").to(SignUpController.class, "create");

                mr.get("/account").to(MyController.class, "account");
                mr.post("/account").to(MyController.class, "changePassword");
                mr.post("/session/:id/revoke").to(MyController.class, "revokeSession");
                mr.post("/2fa/:enabled").to(MyController.class,  "switchTwoFactorAuth");

                /* Invitation*/
                mr.get("/invitation").to(InvitationController.class, "");
                mr.post("/invitation").to(InvitationController.class, "");

                /* OpenID Connect */
                mr.get("/oidc/authorize").to(OidcController.class, "authorize");
                mr.post("/oidc/token").to(OidcController.class, "token");

                mr.get("/").to(MyController.class, "home");

            });

        }).compile();

        // Enkan
        app.use(new DefaultCharsetMiddleware());
        app.use(new MetricsMiddleware<>());
        app.use(NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(envIn("development"), new LazyLoadMiddleware<>("enkan.middleware.devel.StacktraceMiddleware"));
        app.use(envIn("development"), new LazyLoadMiddleware<>("enkan.middleware.devel.TraceWebMiddleware"));
        app.use(new TraceMiddleware<>());
        app.use(new ContentTypeMiddleware());
        app.use(envIn("development"), new LazyLoadMiddleware<>("enkan.middleware.devel.HttpStatusCatMiddleware"));
        app.use(new ParamsMiddleware());
        app.use(new MultipartParamsMiddleware());
        app.use(new MethodOverrideMiddleware());
        app.use(new NormalizationMiddleware());
        app.use(new NestedParamsMiddleware());
        app.use(new CookiesMiddleware());

        app.use(new AuthenticationMiddleware<>(Collections.singletonList(injector.inject(new BouncrStoreBackend()))));
        app.use(and(path("^(/my(?!(/signIn|/signUp|/assets|/oidc))|/admin(?!(/api)))($|/.*)"), authenticated().negate()),
                (Endpoint<HttpRequest, HttpResponse>) req ->
                        HttpResponseUtils.redirect("/my/signIn?url=" + req.getUri(),
                                HttpResponseUtils.RedirectStatusCode.TEMPORARY_REDIRECT));

        app.use(builder(new ContentNegotiationMiddleware())
                .set(ContentNegotiationMiddleware::setAllowedLanguages,
                        new HashSet<>(Arrays.asList("en", "ja")))
                .build());
        // Kotowari
        app.use(new ResourceMiddleware());
        app.use(builder(new RenderTemplateMiddleware())
                .set(RenderTemplateMiddleware::setUserFunctions, userFunctions())
                .build());
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

    private Map<String, Function<List, Object>> userFunctions() {
        Map<String, Function<List, Object>> functions = new HashMap<>();
        functions.put("md5hex", arguments -> {
            if (arguments.size() == 1) {
                String text = Objects.toString(arguments.get(0));
                return DigestUtils.md5hex(text);
            } else {
                throw new MisconfigurationException("bouncr.MD5_HEX_WRONG_ARGS");
            }
        });
        functions.put("parseUserAgent", arguments -> {
           if (arguments.size() < 1) throw new MisconfigurationException("bouncr.PARSE_USER_AGENT_WRONG_ARGS");

           return Classifier.parse(Objects.toString(arguments.get(0)));
        });
        return functions;
    }

}
