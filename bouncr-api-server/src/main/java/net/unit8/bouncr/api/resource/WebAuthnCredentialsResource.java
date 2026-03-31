package net.unit8.bouncr.api.resource;

import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.WebAuthnCredentialRepository;
import net.unit8.bouncr.api.util.PrincipalUtils;
import net.unit8.bouncr.data.User;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "DELETE"})
public class WebAuthnCredentialsResource {

    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<Long> CREDENTIAL_ID = ContextKey.of(Long.class);

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        if (PrincipalUtils.isClientToken(principal)) return false;
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:read") || principal.hasPermission("my:update");
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean allowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:update");
    }

    @Decision(value = MALFORMED, method = "DELETE")
    public Problem validateDelete(HttpRequest request, RestContext context) {
        if (request.getParams() == null || request.getParams().get("id") == null) {
            return Problem.valueOf(400, "id parameter is required", BouncrProblem.MALFORMED.problemUri());
        }
        try {
            context.put(CREDENTIAL_ID, Long.parseLong(request.getParams().get("id")));
        } catch (NumberFormatException e) {
            return Problem.valueOf(400, "id must be a number", BouncrProblem.MALFORMED.problemUri());
        }
        return null;
    }

    @Decision(EXISTS)
    public boolean exists(UserPermissionPrincipal principal, RestContext context, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        var user = userRepo.findByAccount(principal.getName());
        user.ifPresent(u -> context.put(USER, u));
        return user.isPresent();
    }

    @Decision(HANDLE_OK)
    public List<Map<String, Object>> list(User user, DSLContext dsl) {
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        return credRepo.findByUserId(user.id()).stream()
                .map(BouncrJsonEncoders.WEBAUTHN_CREDENTIAL::encode)
                .toList();
    }

    @Decision(DELETE)
    public Void delete(Long credentialId, User user, DSLContext dsl) {
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        credRepo.deleteByUserIdAndCredentialId(user.id(), credentialId);
        return null;
    }
}
