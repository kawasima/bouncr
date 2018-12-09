package net.unit8.bouncr.api;

import enkan.Application;
import enkan.Endpoint;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.endpoint.ResourceEndpoint;
import enkan.exception.MisconfigurationException;
import enkan.middleware.*;
import enkan.middleware.jpa.EntityManagerMiddleware;
import enkan.middleware.metrics.MetricsMiddleware;
import enkan.security.bouncr.BouncrBackend;
import enkan.system.inject.ComponentInjector;
import is.tagomor.woothee.Classifier;
import kotowari.inject.ParameterInjector;
import kotowari.inject.parameter.*;
import kotowari.middleware.RoutingMiddleware;
import kotowari.middleware.SerDesMiddleware;
import kotowari.restful.middleware.ResourceInvokerMiddleware;
import kotowari.routing.Routes;
import net.unit8.bouncr.api.resource.*;
import net.unit8.bouncr.api.resource.me.OtpKeyResource;
import net.unit8.bouncr.api.resource.me.PasswordCredentialResource;
import net.unit8.bouncr.api.resource.me.UserActionsResource;
import net.unit8.bouncr.api.resource.me.UserSessionsResource;
import net.unit8.bouncr.util.DigestUtils;
import org.bouncycastle.crypto.util.PrivateKeyFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
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
                ar.all("/users").to(UsersResource.class);
                ar.all("/user/:account").to(UserResource.class);
                ar.all("/groups").to(GroupsResource.class);
                ar.all("/group/:name").to(GroupResource.class);
                ar.all("/applications").to(ApplicationsResource.class);
                ar.all("/application/:name").to(ApplicationResource.class);
                ar.all("/application/:name/realms").to(UsersResource.class);
                ar.all("/application/:name/realm/:realmName").to(UsersResource.class);
                ar.all("/assignments");
                ar.all("/roles").to(RolesResource.class);
                ar.all("/role/:name").to(RoleResource.class);
                ar.all("/permissions").to(PermissionResource.class);
                ar.all("/permission/:name").to(PermissionResource.class);
                ar.all("/sign_in").to(PasswordSignInResource.class);
                ar.all("/me/password_credential").to(PasswordCredentialResource.class);
                ar.all("/me/otp_key").to(OtpKeyResource.class);
                ar.all("/me/actions").to(UserActionsResource.class);
                ar.all("/me/sessions").to(UserSessionsResource.class);
                ar.all("/me/session/:userSessionId").to(UserSessionsResource.class);
            });

        }).compile();

        List<ParameterInjector<?>> parameterInjectors = List.of(
                new HttpRequestInjector(),
                new ParametersInjector(),
                new SessionInjector(),
                new FlashInjector<>(),
                new PrincipalInjector(),
                new ConversationInjector(),
                new ConversationStateInjector(),
                new LocaleInjector(),
                new EntityManagerInjector()

        );
        // Enkan
        app.use(new DefaultCharsetMiddleware<>());
        app.use(new MetricsMiddleware<>());
        app.use(NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(new TraceMiddleware<>());
        app.use(new ContentTypeMiddleware<>());
        app.use(new ParamsMiddleware<>());
        app.use(new MultipartParamsMiddleware<>());
        app.use(new NestedParamsMiddleware<>());
        app.use(new CookiesMiddleware<>());
        app.use(builder(new CorsMiddleware<>())
                .set(CorsMiddleware::setHeaders, new HashSet<>(Arrays.asList(
                        "Origin", "Accept", "X-Requested-With", "Content-Type",
                        "Access-Control-Request-Method", "Access-Control-Request-Headers",
                        "X-Bouncr-Token")))
                .build());

        try {

            BouncrBackend bouncrBackend = builder(new BouncrBackend())
                    .set(BouncrBackend::setKey, "abcdefghijklmnopqrstuvwxyzabcdef")
                    .build();
            app.use(new AuthenticationMiddleware<>(Collections
                    .singletonList(injector.inject(bouncrBackend))));
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }

        app.use(and(path("^(/api(?!(/sign_in|/sign_up|/oidc)))"),
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
        app.use(new EntityManagerMiddleware<>());
        app.use(new SerDesMiddleware<>());
        app.use(builder(new ResourceInvokerMiddleware<>(injector))
                .set(ResourceInvokerMiddleware::setParameterInjectors, parameterInjectors)
                .set(ResourceInvokerMiddleware::setDefaultResource, new BouncrBaseResource())
                .build());

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

    private PrivateKey generatePrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return pair.getPrivate();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
