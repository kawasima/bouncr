package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

/**
 * Regenerates the client_secret for an OIDC application.
 * The old secret is immediately invalidated.
 */
@AllowedMethods("POST")
public class OidcApplicationSecretResource {
    static final ContextKey<OidcApplication> APP = ContextKey.of(OidcApplication.class);
    static final ContextKey<String> NEW_SECRET = ContextKey.of("newSecret", String.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean allowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:update"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        var app = repo.findByName(params.get("name"));
        app.ifPresent(a -> context.put(APP, a));
        return app.isPresent();
    }

    @Decision(POST)
    public Object regenerate(OidcApplication app, RestContext context, DSLContext dsl) {
        String plaintextSecret = RandomUtils.generateRandomString(32, config.getSecureRandom());
        byte[] secretHash = PasswordUtils.pbkdf2(plaintextSecret, app.clientId(), config.getPbkdf2Iterations());
        String hashedSecret = Base64.getEncoder().encodeToString(secretHash);

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.updateClientSecret(app.name(), hashedSecret);

        context.put(NEW_SECRET, plaintextSecret);
        return true;
    }

    @Decision(HANDLE_CREATED)
    public Map<String, String> handleCreated(String newSecret, OidcApplication app) {
        return Map.of(
                "client_id", app.clientId(),
                "client_secret", newSecret);
    }
}
