package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.AssignmentItem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.AssignmentIdObject;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
    @SuppressWarnings("unchecked")
    static final ContextKey<List<AssignmentItem>> ITEMS = (ContextKey<List<AssignmentItem>>) (ContextKey<?>) ContextKey.of(List.class);

    record ResolvedAssignment(Long groupId, Long roleId, Long realmId) {}

    @SuppressWarnings("unchecked")
    static final ContextKey<List<ResolvedAssignment>> RESOLVED = (ContextKey<List<ResolvedAssignment>>) (ContextKey<?>) ContextKey.of(ArrayList.class);

    private Long resolveId(AssignmentRepository repo, String tableName, String idColumn, AssignmentIdObject idObj) {
        if (idObj.id() != null) {
            return idObj.id();
        } else if (idObj.name() != null) {
            return repo.resolveIdByName(tableName, idColumn, idObj.name());
        }
        return null;
    }

    private Problem validateAndResolve(List<AssignmentItem> items, RestContext context, DSLContext dsl, boolean forDelete) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        List<Problem.Violation> violations = new ArrayList<>();

        // Validate that each item has group, role, realm with id or name
        IntStream.range(0, items.size()).forEach(i -> {
            AssignmentItem item = items.get(i);
            if (item.group() == null || (item.group().id() == null && item.group().name() == null)) {
                violations.add(new Problem.Violation("assignments[" + i + "][group]", "must not be null"));
            }
            if (item.role() == null || (item.role().id() == null && item.role().name() == null)) {
                violations.add(new Problem.Violation("assignments[" + i + "][role]", "must not be null"));
            }
            if (item.realm() == null || (item.realm().id() == null && item.realm().name() == null)) {
                violations.add(new Problem.Violation("assignments[" + i + "][realm]", "must not be null"));
            }
        });

        if (!violations.isEmpty()) {
            return Problem.fromViolationList(violations);
        }

        List<ResolvedAssignment> resolved = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            AssignmentItem item = items.get(i);
            Long groupId = resolveId(repo, "groups", "group_id", item.group());
            Long roleId = resolveId(repo, "roles", "role_id", item.role());
            Long realmId = resolveId(repo, "realms", "realm_id", item.realm());

            if (groupId == null) {
                violations.add(new Problem.Violation("assignments[" + i + "][group]", "not found"));
                continue;
            }
            if (roleId == null) {
                violations.add(new Problem.Violation("assignments[" + i + "][role]", "not found"));
                continue;
            }
            if (realmId == null) {
                violations.add(new Problem.Violation("assignments[" + i + "][realm]", "not found"));
                continue;
            }

            if (forDelete) {
                if (repo.exists(groupId, roleId, realmId)) {
                    resolved.add(new ResolvedAssignment(groupId, roleId, realmId));
                }
            } else {
                resolved.add(new ResolvedAssignment(groupId, roleId, realmId));
            }
        }

        if (!violations.isEmpty()) {
            return Problem.fromViolationList(violations);
        }

        context.put(RESOLVED, resolved);
        return null;
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateForPost(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ASSIGNMENTS.decode(body)) {
            case Ok<List<AssignmentItem>> ok -> validateAndResolve(ok.value(), context, dsl, false);
            case Err<List<AssignmentItem>>(var issues) -> toProblem(issues);
        };
    }

    @Decision(value = MALFORMED, method = "DELETE")
    public Problem validateForDelete(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.ASSIGNMENTS.decode(body)) {
            case Ok<List<AssignmentItem>> ok -> validateAndResolve(ok.value(), context, dsl, true);
            case Err<List<AssignmentItem>>(var issues) -> toProblem(issues);
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
