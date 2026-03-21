package net.unit8.bouncr.api;

import enkan.Application;
import enkan.Env;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.application.WebApplication;
import enkan.config.ApplicationFactory;
import enkan.endpoint.ResourceEndpoint;
import enkan.exception.MisconfigurationException;
import enkan.middleware.*;
import enkan.middleware.metrics.MetricsMiddleware;
import enkan.security.bouncr.BouncrBackend;
import enkan.system.inject.ComponentInjector;
import is.tagomor.woothee.Classifier;
import kotowari.inject.ParameterInjector;
import kotowari.inject.parameter.ConversationInjector;
import kotowari.inject.parameter.ConversationStateInjector;
import kotowari.inject.parameter.FlashInjector;
import kotowari.inject.parameter.HttpRequestInjector;
import kotowari.inject.parameter.LocaleInjector;
import kotowari.inject.parameter.ParametersInjector;
import kotowari.inject.parameter.PrincipalInjector;
import kotowari.inject.parameter.SessionInjector;
import kotowari.middleware.RoutingMiddleware;
import kotowari.middleware.SerDesMiddleware;
import kotowari.restful.middleware.ResourceInvokerMiddleware;
import kotowari.routing.Routes;
import net.unit8.bouncr.api.logging.ActionLoggingMiddleware;
import enkan.middleware.jooq.JooqDslContextMiddleware;
import enkan.middleware.jooq.JooqTransactionMiddleware;
import net.unit8.bouncr.api.inject.DSLContextInjector;
import net.unit8.bouncr.api.logging.ActionRecordInjector;
import net.unit8.bouncr.api.resource.*;
import net.unit8.bouncr.util.DigestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.Predicates.*;

/**
 * The factory for Bouncr application.
 *
 * @author kawasima
 */
public class BouncrApplicationFactory implements ApplicationFactory<HttpRequest, HttpResponse> {
    @Override
    public Application<HttpRequest, HttpResponse> create(ComponentInjector injector) {
        WebApplication app = new WebApplication();

        // Routing
        Routes routes = Routes.define(r -> {
            r.scope("/bouncr", br -> {
                br.all("/problem/:problem").to(ProblemResource.class);
            });

            r.scope("/bouncr/api", ar -> {
                ar.all("/users").to(UsersResource.class);
                ar.all("/user/:account").to(UserResource.class);
                ar.all("/groups").to(GroupsResource.class);
                ar.all("/group/:name").to(GroupResource.class);
                ar.all("/group/:name/users").to(GroupUsersResource.class);
                ar.all("/applications").to(ApplicationsResource.class);
                ar.all("/application/:name").to(ApplicationResource.class);
                ar.all("/application/:name/realms").to(RealmsResource.class);
                ar.all("/application/:name/realm/:realmName").to(RealmResource.class);
                ar.all("/assignments").to(AssignmentsResource.class);
                ar.all("/assignment").to(AssignmentResource.class);
                ar.all("/roles").to(RolesResource.class);
                ar.all("/role/:name").to(RoleResource.class);
                ar.all("/role/:name/permissions").to(RolePermissionsResource.class);
                ar.all("/permissions").to(PermissionsResource.class);
                ar.all("/permission/:name").to(PermissionResource.class);
                ar.all("/oidc_providers").to(OidcProvidersResource.class);
                ar.all("/oidc_provider/:name").to(OidcProviderResource.class);
                ar.all("/oidc_applications").to(OidcApplicationsResoruce.class);
                ar.all("/oidc_application/:name").to(OidcApplicationResource.class);

                ar.all("/invitation/:code").to(InvitationResource.class);
                ar.all("/invitations").to(InvitationsResource.class);

                ar.all("/sign_in").to(PasswordSignInResource.class);
                ar.all("/pre_sign_in").to(PreSignInResource.class);
                ar.all("/sign_in/oidc/:name").to(OidcSignInResource.class);
                ar.all("/sign_in/oidc_authorization/:name").to(OidcAuthorizationResource.class);
                ar.all("/sign_up").to(SignUpResource.class);
                ar.all("/user_profile_verification").to(UserProfileVerificationResource.class);
                ar.all("/password_credential/reset_code").to(PasswordResetChallengeResource.class);
                ar.all("/password_credential/reset").to(PasswordResetResource.class);
                ar.all("/password_credential").to(PasswordCredentialResource.class);
                ar.all("/otp_key").to(OtpKeyResource.class);
                ar.all("/actions").to(UserActionsResource.class);
                // Sessions can't be listed by a user now.
                //ar.all("/sessions").to(UserSessionsResource.class);
                ar.all("/session/:token").to(UserSessionResource.class);
                ar.all("/token/refresh").to(TokenRefreshResource.class);
            });

            // OAuth2/OIDC Identity Provider endpoints
            r.scope("/oauth2", oa -> {
                oa.get("/authorize").to(OAuth2AuthorizeResource.class);
                oa.post("/token").to(OAuth2TokenResource.class);
                oa.get("/openid/:client_id/certs").to(OAuth2JwksResource.class);
                oa.get("/openid/:client_id/.well-known/openid-configuration").to(OAuth2DiscoveryResource.class);
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
                new DSLContextInjector(),
                new ActionRecordInjector()
        );
        // Enkan
        app.use(new DefaultCharsetMiddleware());
        app.use(new MetricsMiddleware<>());
        app.use((java.util.function.Predicate<HttpRequest>) NONE, new ServiceUnavailableMiddleware<>(new ResourceEndpoint("/public/html/503.html")));
        app.use(envIn("development"), new TraceMiddleware<>());
        app.use(new ContentTypeMiddleware());
        app.use(new ParamsMiddleware());
        app.use(new MultipartParamsMiddleware());
        app.use(new NestedParamsMiddleware());
        app.use(new CookiesMiddleware());
        app.use(builder(new CorsMiddleware())
                .set(CorsMiddleware::setHeaders, new HashSet<>(Arrays.asList(
                        "Origin", "Accept", "X-Requested-With", "Content-Type",
                        "Access-Control-Request-Method", "Access-Control-Request-Headers",
                        "Authorization", "Set-Cookie", "Cookie",
                        "X-Bouncr-Token")))
                .set(CorsMiddleware::setOrigins, corsOrigins())
                .build());

        try {
            BouncrBackend bouncrBackend = builder(new BouncrBackend())
                    .set(BouncrBackend::setKey, Env.getString("JWT_SECRET", "abcdefghijklmnopqrstuvwxyzabcdef"))
                    .build();
            app.use(new AuthenticationMiddleware<>(Collections
                    .singletonList(injector.inject(bouncrBackend))));
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }

        app.use(builder(new ContentNegotiationMiddleware())
                .set(ContentNegotiationMiddleware::setAllowedTypes,
                        new HashSet<>(Arrays.asList("application/json", "text/html", "application/x-www-form-urlencoded")))
                .set(ContentNegotiationMiddleware::setAllowedLanguages,
                        new HashSet<>(Arrays.asList("en", "ja")))
                .build());
        // Kotowari
        app.use(builder(new ResourceMiddleware())
                .set(ResourceMiddleware::setUriPrefix, (String) null)
                .build());
        app.use(new RoutingMiddleware(routes));
        app.use(new JooqDslContextMiddleware<>());
        app.use(new JooqTransactionMiddleware<>());
        app.use(new ActionLoggingMiddleware());
        app.use(new SerDesMiddleware<>());

        boolean outputErrorReason = Objects.equals(Env.getString("enkan.env", "development"), "development");
        app.use(builder(new ResourceInvokerMiddleware<>(injector))
                .set(ResourceInvokerMiddleware::setParameterInjectors, parameterInjectors)
                .set(ResourceInvokerMiddleware::setDefaultResource, new BouncrBaseResource())
                .set(ResourceInvokerMiddleware::setOutputErrorReason, outputErrorReason)
                .build());

        return app;
    }

    private Map<String, Function<List<Object>, Object>> userFunctions() {
        Map<String, Function<List<Object>, Object>> functions = new HashMap<>();
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

    private Set<String> corsOrigins() {
        final String corsOrigins = Env.getString("cors.origins", "*");
        return Stream.of(corsOrigins.split(","))
                .map(String::strip)
                .collect(Collectors.toSet());
    }

}
