package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.GroupCreate;
import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.data.Group;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class GroupsResource {
    static final ContextKey<GroupCreate> CREATE_REQ = ContextKey.of(GroupCreate.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.GROUP_CREATE.decode(body)) {
            case Ok<GroupCreate> ok -> { context.put(CREATE_REQ, ok.value()); yield null; }
            case Err<GroupCreate>(var issues) -> toProblem(issues);
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
    public boolean isConflict(GroupCreate createRequest, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        return !repo.isNameUnique(createRequest.name());
    }

    @Decision(HANDLE_OK)
    public List<Group> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        String q = params.get("q");
        int offset = Optional.ofNullable(params.<String>get("offset")).map(Integer::parseInt).orElse(0);
        int limit = Optional.ofNullable(params.<String>get("limit")).map(Integer::parseInt).orElse(10);
        boolean isAdmin = principal.hasPermission("any_group:read");
        return repo.search(q, principal.getId(), isAdmin, offset, limit);
    }

    @Decision(POST)
    public Group create(GroupCreate createRequest, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        return repo.insert(createRequest.name(), createRequest.description());
    }
}
