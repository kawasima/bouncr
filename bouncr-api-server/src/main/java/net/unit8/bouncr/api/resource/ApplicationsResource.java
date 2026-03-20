package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.ApplicationCreate;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.data.Application;
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
    static final ContextKey<ApplicationCreate> CREATE_REQ = ContextKey.of(ApplicationCreate.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.APPLICATION_CREATE.decode(body)) {
            case Ok<ApplicationCreate> ok -> { context.put(CREATE_REQ, ok.value()); yield null; }
            case Err<ApplicationCreate>(var issues) -> toProblem(issues);
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
    public boolean isConflict(ApplicationCreate createRequest, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return !repo.isNameUnique(createRequest.name());
    }

    @Decision(HANDLE_OK)
    public List<Application> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        String q = params.get("q");
        int offset = Optional.ofNullable(params.<String>get("offset")).map(Integer::parseInt).orElse(0);
        int limit = Optional.ofNullable(params.<String>get("limit")).map(Integer::parseInt).orElse(10);
        boolean embedRealms = Objects.equals(params.get("embed"), "realms");
        return repo.search(q, embedRealms, offset, limit);
    }

    @Decision(POST)
    public Application create(ApplicationCreate createRequest, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return repo.insert(createRequest.name(), createRequest.description(),
                createRequest.virtualPath(), createRequest.passTo(), createRequest.topPage());
    }
}
