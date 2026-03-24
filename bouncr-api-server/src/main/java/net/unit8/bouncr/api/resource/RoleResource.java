package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.boundary.RoleUpdate;
import net.unit8.bouncr.api.repository.RoleRepository;
import net.unit8.bouncr.data.Role;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class RoleResource {
    static final ContextKey<RoleUpdate> UPDATE_REQ = ContextKey.of(RoleUpdate.class);
    static final ContextKey<Role> ROLE = ContextKey.of(Role.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ROLE_UPDATE.decode(body)) {
            case Ok<RoleUpdate> ok -> { context.put(UPDATE_REQ, ok.value()); yield null; }
            case Err<RoleUpdate>(var issues) -> toProblem(issues);
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

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:update") || p.hasPermission("any_role:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:delete") || p.hasPermission("any_role:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(RoleUpdate updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest.name(), params.get("name"))) {
            return false;
        }
        RoleRepository repo = new RoleRepository(dsl);
        return !repo.isNameUnique(updateRequest.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        Optional<Role> role = repo.findByName(params.get("name"), false);
        role.ifPresent(r -> context.put(ROLE, r));
        return role.isPresent();
    }

    @Decision(HANDLE_OK)
    public Role find(Role role) {
        return role;
    }

    @Decision(PUT)
    public Role update(RoleUpdate updateRequest, Role role, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        repo.update(role.name(), updateRequest.name(), updateRequest.description());
        return repo.findByName(updateRequest.name(), false).orElseThrow();
    }

    @Decision(DELETE)
    public Void delete(Role role, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        repo.delete(role.name());
        return null;
    }
}
