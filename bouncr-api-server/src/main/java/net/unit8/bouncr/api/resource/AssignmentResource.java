package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.data.Assignment;
import org.jooq.DSLContext;

import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET"})
public class AssignmentResource {
    static final ContextKey<Assignment> ASSIGNMENT = ContextKey.of(Assignment.class);

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("assignments:read"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        AssignmentRepository repo = new AssignmentRepository(dsl);
        Optional<Assignment> assignment = repo.findByGroupRoleRealm(
                params.get("group"), params.get("role"), params.get("realm"));
        assignment.ifPresent(a -> context.put(ASSIGNMENT, a));
        return assignment.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(Assignment assignment) {
        return BouncrJsonEncoders.ASSIGNMENT.encode(assignment);
    }
}
