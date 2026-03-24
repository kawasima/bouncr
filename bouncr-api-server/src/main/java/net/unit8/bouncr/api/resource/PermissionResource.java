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
import net.unit8.bouncr.api.repository.PermissionRepository;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.PermissionName;
import net.unit8.bouncr.api.util.ContextKeys;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple2;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class PermissionResource {
    static final ContextKey<Tuple2<PermissionName, String>> UPDATE_REQ =
            ContextKeys.of(Tuple2.class);
    static final ContextKey<Permission> PERMISSION = ContextKey.of(Permission.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.PERMISSION_UPDATE.decode(body)) {
            case Ok(Tuple2(var name, var desc)) -> {
                context.put(UPDATE_REQ, new Tuple2<>((PermissionName) name, (String) desc));
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
    public boolean isConflict(Tuple2<PermissionName, String> updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest._1().value(), params.get("name"))) {
            return false;
        }
        PermissionRepository repo = new PermissionRepository(dsl);
        return !repo.isNameUnique(updateRequest._1().value());
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
    public Permission update(Tuple2<PermissionName, String> updateRequest, Permission permission, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        repo.update(permission.name(), updateRequest._1().value(), updateRequest._2());
        return repo.findByName(updateRequest._1().value()).orElseThrow();
    }

    @Decision(DELETE)
    public Void delete(Permission permission, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        repo.delete(permission.name());
        return null;
    }
}
