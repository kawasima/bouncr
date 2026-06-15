package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.GroupSpec;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class GroupResource {
    static final ContextKey<GroupSpec> GROUP_SPEC = ContextKey.of(GroupSpec.class);
    static final ContextKey<Group> GROUP = ContextKey.of(Group.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
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

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:update") || p.hasPermission("any_group:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:delete") || p.hasPermission("any_group:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(GroupSpec groupSpec, Parameters params, DSLContext dsl) {
        if (groupSpec.name().matches(params.get("name"))) {
            return false;
        }
        GroupRepository repo = new GroupRepository(dsl);
        return !repo.isNameUnique(groupSpec.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        boolean embedUsers = Objects.equals(params.get("embed"), "users");
        Optional<Group> group = repo.findByName(params.get("name"), embedUsers);
        group.ifPresent(g -> context.put(GROUP, g));
        return group.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(Group group) {
        return BouncrJsonEncoders.GROUP.encode(group);
    }

    @Decision(PUT)
    public Map<String, Object> update(GroupSpec groupSpec, Group group, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        repo.update(group.name(), groupSpec);
        actionRecord.setActionType(ActionType.GROUP_MODIFIED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(group.name().value());
        return BouncrJsonEncoders.GROUP.encode(repo.findByName(groupSpec.name().value(), false).orElseThrow());
    }

    @Decision(DELETE)
    public Object delete(Group group, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        if (Boolean.TRUE.equals(group.writeProtected())) {
            return Problem.valueOf(403, "Cannot delete a write-protected group",
                    BouncrProblem.UNPROCESSABLE.problemUri());
        }
        GroupRepository repo = new GroupRepository(dsl);
        repo.delete(group.name());
        actionRecord.setActionType(ActionType.GROUP_DELETED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(group.name().value());
        return null;
    }
}
