package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.repository.UserSessionRepository;
import net.unit8.bouncr.api.util.PaginationParams;
import net.unit8.bouncr.api.util.PrincipalUtils;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET"})
public class UserSessionsResource {

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        if (PrincipalUtils.isClientToken(principal)) return false;
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean isGetAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read")
                        || p.hasPermission("any_user:read")
                        || (p.hasPermission("my:read") && Objects.equals(p.getName(), params.get("account"))))
                .isPresent();
    }

    @Decision(HANDLE_OK)
    public List<Map<String, Object>> handleOk(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        UserSessionRepository repo = new UserSessionRepository(dsl);
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        return repo.searchByUserId(principal.getId(), offset, limit).stream()
                .map(BouncrJsonEncoders.USER_SESSION::encode)
                .toList();
    }
}
