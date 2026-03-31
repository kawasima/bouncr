package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.data.OidcApplicationUpdateSpec;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.Presence;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.presenceToNullable;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcApplicationResource {
    static final ContextKey<OidcApplicationUpdateSpec> UPDATE_REQ = ContextKey.of(OidcApplicationUpdateSpec.class);
    static final ContextKey<OidcApplication> OIDC_APPLICATION = ContextKey.of(OidcApplication.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_APPLICATION_UPDATE.decode(body)) {
            case Ok<OidcApplicationUpdateSpec> ok -> {
                try {
                    LogoutUriPolicy.normalizeBackchannelLogoutUri(presenceToNullable(ok.value().backchannelLogoutUri()));
                    LogoutUriPolicy.normalizeLogoutUri(presenceToNullable(ok.value().frontchannelLogoutUri()));
                    context.put(UPDATE_REQ, ok.value());
                    yield null;
                } catch (IllegalArgumentException e) {
                    yield Problem.valueOf(400, e.getMessage());
                }
            }
            case Err<OidcApplicationUpdateSpec>(var issues) -> toProblem(issues);
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

    @Decision(value = PROCESSABLE, method = "PUT")
    public boolean isProcessable(OidcApplicationUpdateSpec updateRequest,
                                 UserPermissionPrincipal principal, RestContext context) {
        List<String> requestedPermissions = updateRequest.permissions();
        if (requestedPermissions != null && !requestedPermissions.isEmpty()) {
            var excess = requestedPermissions.stream()
                    .filter(p -> !principal.permissions().contains(p))
                    .toList();
            if (!excess.isEmpty()) {
                context.setMessage(Problem.valueOf(403,
                        "Cannot grant permissions you do not have: " + excess));
                return false;
            }
        }
        return true;
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(OidcApplicationUpdateSpec updateRequest, Parameters params, DSLContext dsl) {
        if (updateRequest.name().matches(params.get("name"))) {
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
        return BouncrJsonEncoders.encodeOidcApplication(oidcApplication);
    }

    @Decision(PUT)
    public Map<String, Object> update(OidcApplicationUpdateSpec updateRequest, OidcApplication oidcApplication, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.updateProfile(
                oidcApplication.name(),
                updateRequest.name().value(),
                toNullableUpdate(updateRequest.homeUri()),
                toNullableUpdate(updateRequest.callbackUri()),
                toNullableUpdate(updateRequest.description()),
                toNullableUpdate(updateRequest.backchannelLogoutUri(),
                        LogoutUriPolicy::normalizeBackchannelLogoutUri),
                toNullableUpdate(updateRequest.frontchannelLogoutUri(),
                        LogoutUriPolicy::normalizeLogoutUri)
        );
        Long appId = repo.findByName(updateRequest.name().value()).map(OidcApplication::id).orElse(oidcApplication.id());
        if (updateRequest.permissions() != null) {
            repo.setPermissions(appId, updateRequest.permissions());
        }
        repo.setGrantTypes(appId, GrantType.parseAll(updateRequest.grantTypes()));
        return BouncrJsonEncoders.encodeOidcApplication(repo.findByName(updateRequest.name().value()).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(OidcApplication oidcApplication, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        repo.delete(oidcApplication.name());
        return null;
    }

    private static <T> OidcApplicationRepository.NullableUpdate<T> toNullableUpdate(Presence<T> p) {
        return switch (p) {
            case Presence.Present<T>(var v) -> OidcApplicationRepository.NullableUpdate.of(v);
            case Presence.PresentNull<?> ignored -> OidcApplicationRepository.NullableUpdate.of(null);
            case Presence.Absent<?> ignored -> OidcApplicationRepository.NullableUpdate.absent();
        };
    }

    private static <T, R> OidcApplicationRepository.NullableUpdate<R> toNullableUpdate(
            Presence<T> p, java.util.function.Function<T, R> transform) {
        return switch (p) {
            case Presence.Present<T>(var v) -> OidcApplicationRepository.NullableUpdate.of(transform.apply(v));
            case Presence.PresentNull<?> ignored -> OidcApplicationRepository.NullableUpdate.of(null);
            case Presence.Absent<?> ignored -> OidcApplicationRepository.NullableUpdate.absent();
        };
    }
}
