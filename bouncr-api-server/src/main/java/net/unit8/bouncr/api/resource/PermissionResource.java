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
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.PermissionRepository;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.PermissionSpec;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class PermissionResource {
    static final ContextKey<PermissionSpec> PERMISSION_SPEC = ContextKey.of(PermissionSpec.class);
    static final ContextKey<Permission> PERMISSION = ContextKey.of(Permission.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.PERMISSION_SPEC.decode(body)) {
            case Ok(var spec) -> {
                context.put(PERMISSION_SPEC, spec);
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
    public boolean isConflict(PermissionSpec permissionSpec, Parameters params, DSLContext dsl) {
        if (permissionSpec.name().matches(params.get("name"))) {
            return false;
        }
        PermissionRepository repo = new PermissionRepository(dsl);
        return !repo.isNameUnique(permissionSpec.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        Optional<Permission> permission = repo.findByName(params.get("name"));
        permission.ifPresent(p -> context.put(PERMISSION, p));
        return permission.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(Permission permission) {
        return BouncrJsonEncoders.PERMISSION.encode(permission);
    }

    @Decision(PUT)
    public Map<String, Object> update(PermissionSpec permissionSpec, Permission permission, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        repo.update(permission.name(), permissionSpec);
        return BouncrJsonEncoders.PERMISSION.encode(repo.findByName(permissionSpec.name().value()).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(Permission permission, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        repo.delete(permission.name());
        return null;
    }
}
