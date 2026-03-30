package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.ApplicationSpec;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class ApplicationResource {
    static final ContextKey<ApplicationSpec> APPLICATION_SPEC = ContextKey.of(ApplicationSpec.class);
    static final ContextKey<Application> APPLICATION = ContextKey.of(Application.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.APPLICATION_SPEC.decode(body)) {
            case Ok(var applicationSpec) -> {
                context.put(APPLICATION_SPEC, applicationSpec);
                yield null;
            }
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("application:read") || p.hasPermission("any_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("application:update") || p.hasPermission("any_application:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("application:delete") || p.hasPermission("any_application:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(ApplicationSpec applicationSpec, Parameters params, DSLContext dsl) {
        if (applicationSpec.name().matches(params.get("name"))) {
            return false;
        }
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return !repo.isNameUnique(applicationSpec.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        boolean embedRealms = Objects.equals(params.get("embed"), "realms");
        Optional<Application> application = repo.findByName(new WordName(params.get("name")), embedRealms);
        application.ifPresent(a -> context.put(APPLICATION, a));
        return application.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(Application application) {
        return BouncrJsonEncoders.APPLICATION.encode(application);
    }

    @Decision(PUT)
    public Map<String, Object> update(ApplicationSpec spec, Application application, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        repo.update(application.name(), spec);
        return BouncrJsonEncoders.APPLICATION.encode(repo.findByName(spec.name(), false).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(Application application, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        repo.delete(application.name());
        return null;
    }
}
