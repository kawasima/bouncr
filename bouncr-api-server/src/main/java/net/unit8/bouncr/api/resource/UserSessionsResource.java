package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.UserSessionRepository;
import net.unit8.bouncr.data.UserSession;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET"})
public class UserSessionsResource {

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
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
    public List<UserSession> handleOk(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        UserSessionRepository repo = new UserSessionRepository(dsl);
        int offset = Optional.ofNullable(params.<String>get("offset")).map(Integer::parseInt).orElse(0);
        int limit = Optional.ofNullable(params.<String>get("limit")).map(Integer::parseInt).orElse(10);
        return repo.searchByUserId(principal.getId(), offset, limit);
    }
}
