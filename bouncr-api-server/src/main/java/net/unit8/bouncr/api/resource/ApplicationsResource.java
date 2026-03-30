package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.util.PaginationParams;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.ApplicationSpec;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class ApplicationsResource {
    static final ContextKey<ApplicationSpec> APPLICATION_SPEC = ContextKey.of(ApplicationSpec.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
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

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("any_application:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(ApplicationSpec applicationSpec, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return !repo.isNameUnique(applicationSpec.name());
    }

    @Decision(HANDLE_OK)
    public List<Application> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        boolean embedRealms = Objects.equals(params.get("embed"), "realms");
        return repo.search(q, embedRealms, offset, limit);
    }

    @Decision(POST)
    public Application create(ApplicationSpec applicationSpec, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return repo.insert(applicationSpec);
    }
}
