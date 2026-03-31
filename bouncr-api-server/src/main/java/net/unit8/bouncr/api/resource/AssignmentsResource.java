package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.data.AssignmentId;
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
    static final ContextKey<List<AssignmentId>> RESOLVED = ContextKeys.of(List.class);

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
                        .filter(a -> repo.exists(a.group(), a.role(), a.realm()))
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
    public Void create(List<AssignmentId> resolved, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        for (AssignmentId a : resolved) {
            repo.insert(a.group(), a.role(), a.realm());
        }
        return null;
    }

    @Decision(DELETE)
    public Void delete(List<AssignmentId> resolved, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        for (AssignmentId a : resolved) {
            repo.delete(a.group(), a.role(), a.realm());
        }
        return null;
    }
}
