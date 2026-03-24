package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.boundary.ResolvedAssignment;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.api.util.ContextKeys;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

/**
 * Assignments Resource.
 *
 * This resource has
 *
 * <ul>
 *   <li>POST: Create multiple assignments</li>
 *   <li>DELETE: Delete multiple assignments</li>
 * </ul>
 *
 * @author kawasima
 */
@AllowedMethods({"POST", "DELETE"})
public class AssignmentsResource {
    static final ContextKey<List<ResolvedAssignment>> RESOLVED = ContextKeys.of(List.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateForPost(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        AssignmentRepository repo = new AssignmentRepository(dsl);
        return switch (BouncrJsonDecoders.assignments(repo).decode(body)) {
            case Ok(var resolved) -> { context.put(RESOLVED, resolved); yield null; }
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(value = MALFORMED, method = "DELETE")
    public Problem validateForDelete(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        AssignmentRepository repo = new AssignmentRepository(dsl);
        return switch (BouncrJsonDecoders.assignments(repo).decode(body)) {
            case Ok(var resolved) -> {
                var existing = resolved.stream()
                        .filter(a -> repo.exists(a.groupId(), a.roleId(), a.realmId()))
                        .toList();
                context.put(RESOLVED, existing);
                yield null;
            }
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean allowedPost(UserPermissionPrincipal principal) {
        return principal.hasPermission("assignments:create");
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean allowedDelete(UserPermissionPrincipal principal) {
        return principal.hasPermission("assignments:delete");
    }

    @Decision(POST)
    public Void create(List<ResolvedAssignment> resolved, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        for (ResolvedAssignment a : resolved) {
            repo.insert(a.groupId(), a.roleId(), a.realmId());
        }
        return null;
    }

    @Decision(DELETE)
    public Void delete(List<ResolvedAssignment> resolved, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        for (ResolvedAssignment a : resolved) {
            repo.delete(a.groupId(), a.roleId(), a.realmId());
        }
        return null;
    }
}
