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
import net.unit8.bouncr.api.repository.PermissionRepository;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.PermissionName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple2;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class PermissionsResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple2<PermissionName, String>> CREATE_REQ =
            (ContextKey<Tuple2<PermissionName, String>>) (ContextKey<?>) ContextKey.of(Tuple2.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.PERMISSION_CREATE.decode(body)) {
            case Ok(Tuple2(var name, var desc)) -> {
                context.put(CREATE_REQ, new Tuple2<>((PermissionName) name, (String) desc));
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
                .filter(p -> p.hasPermission("permission:read") || p.hasPermission("any_permission:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:create") || p.hasPermission("any_permission:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(Tuple2<PermissionName, String> createRequest, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        return !repo.isNameUnique(createRequest._1().value());
    }

    @Decision(HANDLE_OK)
    public List<Permission> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        boolean isAdmin = principal.hasPermission("any_permission:read");
        return repo.search(q, principal.getId(), isAdmin, offset, limit);
    }

    @Decision(POST)
    public Permission create(Tuple2<PermissionName, String> createRequest, DSLContext dsl) {
        PermissionRepository repo = new PermissionRepository(dsl);
        return repo.insert(createRequest._1().value(), createRequest._2());
    }
}
