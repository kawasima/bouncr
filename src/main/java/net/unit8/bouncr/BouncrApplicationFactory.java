package net.unit8.bouncr;

import enkan.Application;
import enkan.Endpoint;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.endpoint.ResourceEndpoint;
import enkan.exception.MisconfigurationException;
import enkan.middleware.*;
import enkan.middleware.metrics.MetricsMiddleware;
import enkan.system.inject.ComponentInjector;
import is.tagomor.woothee.Classifier;
import kotowari.middleware.*;
import kotowari.middleware.serdes.ToStringBodyWriter;
import kotowari.routing.Routes;
import net.unit8.bouncr.authn.BouncrStoreBackend;
import net.unit8.bouncr.authz.AuthorizeControllerMethodMiddleware;
import net.unit8.bouncr.util.DigestUtils;
import net.unit8.bouncr.web.resource.*;

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
            r.scope("/api", ar -> {
                r.all("/users").to(UsersResource.class);
                r.all("/user/:account").to(UserResource.class);
                r.all("/groups").to(GroupsResource.class);
                r.all("/group/:name").to(GroupResource.class);
                r.all("/applications").to(ApplicationsResource.class);
                r.all("/application/:name").to(ApplicationResource.class);
                r.all("/application/:name/realms").to(UsersResource.class);
                r.all("/application/:name/realm/:realmName").to(UsersResource.class);
                r.all("/roles").to(RolesResource.class);
                r.all("/role/:name").to(RoleResource.class);
                r.all("/permissions").to(PermissionResource.class);
                r.all("/permission/:name").to(PermissionResource.class);
                r.all("/sign_in").to(PasswordSignInResource.class);
            });

        }).compile();

        // Enkan
        app.use(new DefaultCharsetMiddleware<>());
        app.use(new MetricsMiddleware<>());
        app.use(NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(envIn("development"), new LazyLoadMiddleware<>("enkan.middleware.devel.StacktraceMiddleware"));
        app.use(envIn("development"), new LazyLoadMiddleware<>("enkan.middleware.devel.TraceWebMiddleware"));
        app.use(new TraceMiddleware<>());
        app.use(new ContentTypeMiddleware<>());
        app.use(envIn("development"), new LazyLoadMiddleware<>("enkan.middleware.devel.HttpStatusCatMiddleware"));
        app.use(new ParamsMiddleware<>());
        app.use(new MultipartParamsMiddleware<>());
        app.use(new MethodOverrideMiddleware<>());
        app.use(new NormalizationMiddleware<>());
        app.use(new NestedParamsMiddleware<>());
        app.use(new CookiesMiddleware<>());
        app.use(builder(new CorsMiddleware<>())
                .set(CorsMiddleware::setHeaders, new HashSet<>(Arrays.asList(
                        "Origin", "Accept", "X-Requested-With", "Content-Type",
                        "Access-Control-Request-Method", "Access-Control-Request-Headers",
                        "X-Bouncr-Token")))
                .build());

        app.use(new AuthenticationMiddleware<>(Collections.singletonList(injector.inject(new BouncrStoreBackend()))));
        app.use(and(path("^(/my(?!(/signIn|/signUp|/assets|/oidc))|/admin(?!(/api)))($|/.*)"),
                authenticated().negate()),
                (Endpoint<HttpRequest, HttpResponse>) req ->
                        builder(HttpResponse.of(""))
                                .set(HttpResponse::setStatus, 401)
                                .set(HttpResponse::setContentType, "application/json")
                                .build());

        app.use(builder(new ContentNegotiationMiddleware<>())
                .set(ContentNegotiationMiddleware::setAllowedLanguages,
                        new HashSet<>(Arrays.asList("en", "ja")))
                .build());
        // Kotowari
        app.use(builder(new ResourceMiddleware<>())
                .set(ResourceMiddleware::setUriPrefix, (String) null)
                .build());
        app.use(new RoutingMiddleware<>(routes));
        app.use(new AuthorizeControllerMethodMiddleware<>());
        app.use(new FormMiddleware<>());
        app.use(builder(new SerDesMiddleware<>())
                .set(SerDesMiddleware::setBodyWriters, new ToStringBodyWriter())
                .build());
        app.use(new ValidateBodyMiddleware<>());
        app.use(new ControllerInvokerMiddleware<>(injector));

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
