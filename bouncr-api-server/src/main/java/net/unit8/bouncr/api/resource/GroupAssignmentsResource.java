package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.data.Assignment;
import net.unit8.bouncr.data.Group;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods("GET")
public class GroupAssignmentsResource {

    static final ContextKey<Group> GROUP = ContextKey.of(Group.class);

    @Decision(AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean allowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("assignments:read") || p.hasPermission("group:read") || p.hasPermission("any_group:read"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        var group = repo.findByName(params.get("name"), false);
        group.ifPresent(g -> context.put(GROUP, g));
        return group.isPresent();
    }

    @Decision(HANDLE_OK)
    public List<Assignment> list(Group group, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        return repo.findByGroup(group.id());
    }
}
