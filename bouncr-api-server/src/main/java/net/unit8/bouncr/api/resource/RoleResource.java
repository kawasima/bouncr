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
import net.unit8.bouncr.api.repository.RoleRepository;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.Role;
import net.unit8.bouncr.data.RoleSpec;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class RoleResource {
    static final ContextKey<RoleSpec> ROLE_SPEC = ContextKey.of(RoleSpec.class);
    static final ContextKey<Role> ROLE = ContextKey.of(Role.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ROLE_SPEC.decode(body)) {
            case Ok(var spec) -> {
                context.put(ROLE_SPEC, spec);
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
    public boolean isConflict(RoleSpec roleSpec, Parameters params, DSLContext dsl) {
        if (roleSpec.name().matches(params.get("name"))) {
            return false;
        }
        RoleRepository repo = new RoleRepository(dsl);
        return !repo.isNameUnique(roleSpec.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        Optional<Role> role = repo.findByName(params.get("name"), false);
        role.ifPresent(r -> context.put(ROLE, r));
        return role.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(Role role) {
        return BouncrJsonEncoders.ROLE.encode(role);
    }

    @Decision(PUT)
    public Map<String, Object> update(RoleSpec roleSpec, Role role, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        repo.update(role.name(), roleSpec);
        actionRecord.setActionType(ActionType.ROLE_MODIFIED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(role.name().value());
        return BouncrJsonEncoders.ROLE.encode(repo.findByName(roleSpec.name().value(), false).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(Role role, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        repo.delete(role.name());
        actionRecord.setActionType(ActionType.ROLE_DELETED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(role.name().value());
        return null;
    }
}
