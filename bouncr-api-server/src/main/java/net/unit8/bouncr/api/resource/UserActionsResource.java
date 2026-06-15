package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.repository.UserActionRepository;
import net.unit8.bouncr.api.util.PaginationParams;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods("GET")
public class UserActionsResource {

    @Decision(AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean isGetAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read")
                        || p.hasPermission("any_user:read")
                        || (p.hasPermission("my:read") && Objects.equals(p.getName(), params.get("actor"))))
                .isPresent();
    }

    @Decision(HANDLE_OK)
    public List<Map<String, Object>> handleOk(Parameters params,
                                              UserPermissionPrincipal principal,
                                              DSLContext dsl) {
        UserActionRepository repo = new UserActionRepository(dsl);
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);

        String actor;
        if (principal.hasPermission("any_user:read") && params.get("actor") != null) {
            actor = params.get("actor");
        } else {
            actor = principal.getName();
        }
        return repo.search(actor, offset, limit).stream()
                .map(BouncrJsonEncoders.USER_ACTION::encode)
                .toList();
    }
}
