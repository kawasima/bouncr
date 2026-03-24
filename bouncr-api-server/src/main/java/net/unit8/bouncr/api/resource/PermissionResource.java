package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.boundary.PermissionUpdate;
import net.unit8.bouncr.api.repository.PermissionRepository;
import net.unit8.bouncr.data.Permission;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class PermissionResource {
    static final ContextKey<PermissionUpdate> UPDATE_REQ = ContextKey.of(PermissionUpdate.class);
    static final ContextKey<Permission> PERMISSION = ContextKey.of(Permission.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.PERMISSION_UPDATE.decode(body)) {
            case Ok<PermissionUpdate> ok -> { context.put(UPDATE_REQ, ok.value()); yield null; }
            case Err<PermissionUpdate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:read") || p.hasPermission("any_permission:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:update") || p.hasPermission("any_permission:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:delete") || p.hasPermission("any_permission:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(PermissionUpdate updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest.name(), params.get("name"))) {
            return false;
        }
        PermissionRepository repo = new PermissionRepository(dsl);
        return !repo.isNameUnique(updateRequest.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        Optional<Permission> permission = repo.findByName(params.get("name"));
        permission.ifPresent(p -> context.put(PERMISSION, p));
        return permission.isPresent();
    }

    @Decision(HANDLE_OK)
    public Permission find(Permission permission) {
        return permission;
    }

    @Decision(PUT)
    public Permission update(PermissionUpdate updateRequest, Permission permission, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        repo.update(permission.name(), updateRequest.name(), updateRequest.description());
        return repo.findByName(updateRequest.name()).orElseThrow();
    }

    @Decision(DELETE)
    public Void delete(Permission permission, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        repo.delete(permission.name());
        return null;
    }
}
