package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.OidcApplicationUpdate;
import net.unit8.bouncr.api.boundary.OidcApplicationResponse;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcApplicationResource {
    static final ContextKey<OidcApplicationUpdate> UPDATE_REQ = ContextKey.of(OidcApplicationUpdate.class);
    static final ContextKey<OidcApplication> OIDC_APPLICATION = ContextKey.of(OidcApplication.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_APPLICATION_UPDATE.decode(body)) {
            case Ok<OidcApplicationUpdate> ok -> {
                try {
                    LogoutUriPolicy.normalizeBackchannelLogoutUri(ok.value().backchannelLogoutUri());
                    LogoutUriPolicy.normalizeLogoutUri(ok.value().frontchannelLogoutUri());
                    context.put(UPDATE_REQ, ok.value());
                    yield null;
                } catch (IllegalArgumentException e) {
                    yield Problem.valueOf(400, e.getMessage());
                }
            }
            case Err<OidcApplicationUpdate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(OidcApplicationUpdate updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest.name(), params.get("name"))) {
            return false;
        }
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        return !repo.isNameUnique(updateRequest.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        Optional<OidcApplication> oidcApplication = repo.findByName(params.get("name"));
        oidcApplication.ifPresent(a -> context.put(OIDC_APPLICATION, a));
        return oidcApplication.isPresent();
    }

    @Decision(HANDLE_OK)
    public OidcApplicationResponse find(OidcApplication oidcApplication) {
        return OidcApplicationResponse.of(oidcApplication);
    }

    @Decision(PUT)
    public OidcApplicationResponse update(OidcApplicationUpdate updateRequest, OidcApplication oidcApplication, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.update(
                oidcApplication.name(),
                updateRequest.name(),
                null, // clientId not updated
                null, // clientSecret not updated
                null, // privateKey not updated
                null, // publicKey not updated
                updateRequest.homeUrl(),
                updateRequest.callbackUrl(),
                updateRequest.description(),
                LogoutUriPolicy.normalizeBackchannelLogoutUri(updateRequest.backchannelLogoutUri()),
                LogoutUriPolicy.normalizeLogoutUri(updateRequest.frontchannelLogoutUri()),
                updateRequest.hasHomeUrl(),
                updateRequest.hasCallbackUrl(),
                updateRequest.hasDescription(),
                updateRequest.hasBackchannelLogoutUri(),
                updateRequest.hasFrontchannelLogoutUri()
        );
        Long appId = repo.findByName(updateRequest.name()).map(OidcApplication::id).orElse(oidcApplication.id());
        if (updateRequest.permissions() != null) {
            repo.setPermissions(appId, updateRequest.permissions());
        }
        repo.setGrantTypes(appId, GrantType.parseAll(updateRequest.grantTypes()));
        return OidcApplicationResponse.of(repo.findByName(updateRequest.name()).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(OidcApplication oidcApplication, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.delete(oidcApplication.name());
        return null;
    }
}
