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
import net.unit8.bouncr.api.boundary.RealmCreate;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.api.repository.RealmRepository;
import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Realm;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class RealmsResource {
    static final ContextKey<RealmCreate> CREATE_REQ = ContextKey.of(RealmCreate.class);
    static final ContextKey<Application> APPLICATION = ContextKey.of(Application.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.REALM_CREATE.decode(body)) {
            case Ok<RealmCreate> ok -> { context.put(CREATE_REQ, ok.value()); yield null; }
            case Err<RealmCreate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:read") || p.hasPermission("any_realm:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:create") || p.hasPermission("any_realm:create"))
                .isPresent();
    }

    @Decision(PROCESSABLE)
    public boolean isProcessable(Parameters params, RestContext context, DSLContext dsl) {
        ApplicationRepository appRepo = new ApplicationRepository(dsl);
        Optional<Application> application = appRepo.findByName(params.get("name"), false);
        application.ifPresent(a -> context.put(APPLICATION, a));
        return application.isPresent();
    }

    @Decision(POST)
    public Realm create(RealmCreate createRequest, Application application, DSLContext dsl) {
        RealmRepository repo = new RealmRepository(dsl);
        return repo.insert(application.id(), createRequest.name(), createRequest.url(), createRequest.description());
    }

    @Decision(HANDLE_OK)
    public List<Realm> list(Parameters params, Application application, DSLContext dsl) {
        RealmRepository repo = new RealmRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        return repo.search(application.name(), q, offset, limit);
    }
}
