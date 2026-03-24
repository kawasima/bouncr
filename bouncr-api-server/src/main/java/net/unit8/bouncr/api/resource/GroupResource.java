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
import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple3;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class GroupResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple3<WordName, String, List<String>>> UPDATE_REQ =
            (ContextKey<Tuple3<WordName, String, List<String>>>) (ContextKey<?>) ContextKey.of(Tuple3.class);
    static final ContextKey<Group> GROUP = ContextKey.of(Group.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.GROUP_UPDATE.decode(body)) {
            case Ok(Tuple3(var name, var desc, var users)) -> {
                @SuppressWarnings("unchecked")
                var typedUsers = (List<String>) users;
                context.put(UPDATE_REQ, new Tuple3<>((WordName) name, (String) desc, typedUsers));
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
    public boolean isConflict(Tuple3<WordName, String, List<String>> updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest._1().value(), params.get("name"))) {
            return false;
        }
        GroupRepository repo = new GroupRepository(dsl);
        return !repo.isNameUnique(updateRequest._1().value());
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
    public Group find(Group group) {
        return group;
    }

    @Decision(PUT)
    public Group update(Tuple3<WordName, String, List<String>> updateRequest, Group group, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        repo.update(group.name(), updateRequest._1().value(), updateRequest._2());
        return repo.findByName(updateRequest._1().value(), false).orElseThrow();
    }

    @Decision(DELETE)
    public Object delete(Group group, DSLContext dsl) {
        if (Boolean.TRUE.equals(group.writeProtected())) {
            return Problem.valueOf(403, "Cannot delete a write-protected group",
                    BouncrProblem.UNPROCESSABLE.problemUri());
        }
        GroupRepository repo = new GroupRepository(dsl);
        repo.delete(group.name());
        return null;
    }
}
