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
import net.unit8.bouncr.api.util.PaginationParams;
import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.GroupSpec;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class GroupsResource {
    static final ContextKey<GroupSpec> GROUP_SPEC = ContextKey.of(GroupSpec.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.GROUP_SPEC.decode(body)) {
            case Ok(var spec) -> {
                context.put(GROUP_SPEC, spec);
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
                .filter(p -> p.hasPermission("group:read") || p.hasPermission("any_group:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:create") || p.hasPermission("any_group:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(GroupSpec groupSpec, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        return !repo.isNameUnique(groupSpec.name());
    }

    @Decision(HANDLE_OK)
    public List<Map<String, Object>> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        boolean isAdmin = principal.hasPermission("any_group:read");
        return repo.search(q, principal.getId(), isAdmin, offset, limit).stream()
                .map(BouncrJsonEncoders.GROUP::encode)
                .toList();
    }

    @Decision(POST)
    public Map<String, Object> create(GroupSpec groupSpec, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        actionRecord.setActionType(ActionType.GROUP_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(groupSpec.name().value());
        return BouncrJsonEncoders.GROUP.encode(repo.insert(groupSpec));
    }
}
