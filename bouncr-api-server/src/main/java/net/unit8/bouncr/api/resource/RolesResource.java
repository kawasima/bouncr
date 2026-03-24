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
import net.unit8.bouncr.api.boundary.RoleCreate;
import net.unit8.bouncr.api.repository.RoleRepository;
import net.unit8.bouncr.data.Role;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class RolesResource {
    static final ContextKey<RoleCreate> CREATE_REQ = ContextKey.of(RoleCreate.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ROLE_CREATE.decode(body)) {
            case Ok<RoleCreate> ok -> { context.put(CREATE_REQ, ok.value()); yield null; }
            case Err<RoleCreate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:read") || p.hasPermission("any_role:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:create") || p.hasPermission("any_role:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(RoleCreate createRequest, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        return !repo.isNameUnique(createRequest.name());
    }

    @Decision(HANDLE_OK)
    public List<Role> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        boolean isAdmin = principal.hasPermission("any_role:read");
        return repo.search(q, principal.getId(), isAdmin, offset, limit);
    }

    @Decision(POST)
    public Role create(RoleCreate createRequest, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        return repo.insert(createRequest.name(), createRequest.description());
    }
}
