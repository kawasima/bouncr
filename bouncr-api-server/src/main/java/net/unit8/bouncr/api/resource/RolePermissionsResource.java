package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.repository.RolePermissionRepository;
import net.unit8.bouncr.api.repository.RoleRepository;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.Role;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import net.unit8.bouncr.api.repository.PermissionRepository;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST", "DELETE"})
public class RolePermissionsResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<List<String>> PERMISSION_NAMES = (ContextKey<List<String>>) (ContextKey<?>) ContextKey.of(List.class);
    static final ContextKey<Role> ROLE = ContextKey.of(Role.class);

    @Decision(value = MALFORMED, method = {"POST", "DELETE"})
    public Problem validateRequest(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ROLE_PERMISSIONS.decode(body)) {
            case Ok<List<String>> ok -> { context.put(PERMISSION_NAMES, ok.value()); yield null; }
            case Err<List<String>>(var issues) -> toProblem(issues);
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

    @Decision(value = ALLOWED, method = {"POST", "DELETE"})
    public boolean isModifyAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("role:update") || p.hasPermission("any_role:update"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        Optional<Role> role = repo.findByName(params.get("name"), false);
        role.ifPresent(r -> context.put(ROLE, r));
        return role.isPresent();
    }

    @Decision(HANDLE_OK)
    public List<Permission> list(Role role, DSLContext dsl) {
        RolePermissionRepository repo = new RolePermissionRepository(dsl);
        return repo.findPermissionsByRole(role.name());
    }

    @Decision(POST)
    public List<String> create(List<String> permissionNames, Role role, DSLContext dsl) {
        RolePermissionRepository repo = new RolePermissionRepository(dsl);
        List<Long> permissionIds = findPermissionIdsByNames(dsl, permissionNames);
        for (Long permissionId : permissionIds) {
            repo.addPermission(role.id(), permissionId);
        }
        return permissionNames;
    }

    @Decision(DELETE)
    public List<String> delete(List<String> permissionNames, Role role, DSLContext dsl) {
        RolePermissionRepository repo = new RolePermissionRepository(dsl);
        List<Long> permissionIds = findPermissionIdsByNames(dsl, permissionNames);
        for (Long permissionId : permissionIds) {
            repo.removePermission(role.id(), permissionId);
        }
        return permissionNames;
    }

    private List<Long> findPermissionIdsByNames(DSLContext dsl, List<String> names) {
        return new PermissionRepository(dsl).findIdsByNames(names);
    }
}
