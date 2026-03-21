package net.unit8.bouncr.api.resource;

import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.WebAuthnCredentialRepository;
import net.unit8.bouncr.data.User;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "DELETE"})
public class WebAuthnCredentialsResource {

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean allowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:update");
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
    public Void delete(UserPermissionPrincipal principal,
                       HttpRequest request,
                       DSLContext dsl) {
        if (request.getParams() == null) return null;
        String idParam = request.getParams().get("id");
        if (idParam == null) return null;

        long credentialId;
        try {
            credentialId = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            return null;
        }

        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.findByAccount(principal.getName()).orElseThrow();
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        credRepo.deleteByUserIdAndCredentialId(user.id(), credentialId);
        return null;
    }
}
