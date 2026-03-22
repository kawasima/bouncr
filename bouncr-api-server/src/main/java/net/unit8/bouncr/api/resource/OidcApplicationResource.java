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
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcApplicationResource {
    static final ContextKey<OidcApplicationUpdate> UPDATE_REQ = ContextKey.of(OidcApplicationUpdate.class);
    static final ContextKey<OidcApplication> OIDC_APPLICATION = ContextKey.of(OidcApplication.class);
    static final ContextKey<Boolean> HAS_BACKCHANNEL_LOGOUT_URI = ContextKey.of("hasBackchannelLogoutUri", Boolean.class);
    static final ContextKey<Boolean> HAS_FRONTCHANNEL_LOGOUT_URI = ContextKey.of("hasFrontchannelLogoutUri", Boolean.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_APPLICATION_UPDATE.decode(body)) {
            case Ok<OidcApplicationUpdate> ok -> {
                try {
                    context.put(HAS_BACKCHANNEL_LOGOUT_URI, body.get("backchannel_logout_uri") != null);
                    context.put(HAS_FRONTCHANNEL_LOGOUT_URI, body.get("frontchannel_logout_uri") != null);
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
    public Map<String, Object> find(OidcApplication oidcApplication) {
        return sanitize(oidcApplication);
    }

    @Decision(PUT)
    public Map<String, Object> update(OidcApplicationUpdate updateRequest, OidcApplication oidcApplication, RestContext context, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        boolean hasBackchannelLogoutUri = Boolean.TRUE.equals(context.get(HAS_BACKCHANNEL_LOGOUT_URI));
        boolean hasFrontchannelLogoutUri = Boolean.TRUE.equals(context.get(HAS_FRONTCHANNEL_LOGOUT_URI));
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
                hasBackchannelLogoutUri,
                hasFrontchannelLogoutUri
        );
        if (updateRequest.permissions() != null) {
            Long appId = repo.findByName(updateRequest.name()).map(OidcApplication::id).orElse(oidcApplication.id());
            repo.setPermissions(appId, updateRequest.permissions());
        }
        return sanitize(repo.findByName(updateRequest.name()).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(OidcApplication oidcApplication, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.delete(oidcApplication.name());
        return null;
    }

    /**
     * Strip sensitive fields (client_secret hash, private_key) from API responses.
     */
    static Map<String, Object> sanitize(OidcApplication app) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", app.id());
        result.put("name", app.name());
        result.put("client_id", app.clientId());
        result.put("home_url", app.homeUrl());
        result.put("callback_url", app.callbackUrl());
        result.put("description", app.description());
        result.put("backchannel_logout_uri", app.backchannelLogoutUri());
        result.put("frontchannel_logout_uri", app.frontchannelLogoutUri());
        if (app.permissions() != null) {
            result.put("permissions", app.permissions());
        }
        return result;
    }
}
