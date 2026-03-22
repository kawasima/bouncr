package net.unit8.bouncr.api.resource;

import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.WebAuthnCredentialRepository;
import net.unit8.bouncr.data.User;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "DELETE"})
public class WebAuthnCredentialsResource {

    static final ContextKey<Long> CREDENTIAL_ID = ContextKey.of(Long.class);

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
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

    @Decision(HANDLE_OK)
    public List<Map<String, Object>> list(UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.findByAccount(principal.getName()).orElseThrow();
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        return credRepo.findByUserId(user.id()).stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.id(),
                        "credential_name", c.credentialName() != null ? c.credentialName() : "",
                        "transports", c.transports() != null ? c.transports() : "",
                        "discoverable", c.discoverable()))
                .toList();
    }

    @Decision(DELETE)
    public Void delete(Long credentialId,
                       UserPermissionPrincipal principal,
                       DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.findByAccount(principal.getName()).orElseThrow();
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        credRepo.deleteByUserIdAndCredentialId(user.id(), credentialId);
        return null;
    }
}
