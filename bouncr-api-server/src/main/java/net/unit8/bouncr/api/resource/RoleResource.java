package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.repository.RoleRepository;
import net.unit8.bouncr.data.Role;
import net.unit8.bouncr.data.WordName;
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
public class RoleResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple2<WordName, String>> UPDATE_REQ =
            (ContextKey<Tuple2<WordName, String>>) (ContextKey<?>) ContextKey.of(Tuple2.class);
    static final ContextKey<Role> ROLE = ContextKey.of(Role.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ROLE_UPDATE.decode(body)) {
            case Ok(Tuple2(var name, var desc)) -> {
                context.put(UPDATE_REQ, new Tuple2<>((WordName) name, (String) desc));
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException();
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
    public boolean isConflict(Tuple2<WordName, String> updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest._1().value(), params.get("name"))) {
            return false;
        }
        RoleRepository repo = new RoleRepository(dsl);
        return !repo.isNameUnique(updateRequest._1().value());
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
    public Role update(Tuple2<WordName, String> updateRequest, Role role, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        repo.update(role.name(), updateRequest._1().value(), updateRequest._2());
        return repo.findByName(updateRequest._1().value(), false).orElseThrow();
    }

    @Decision(DELETE)
    public Void delete(Role role, DSLContext dsl) {
        RoleRepository repo = new RoleRepository(dsl);
        repo.delete(role.name());
        return null;
    }
}
