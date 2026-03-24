package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.boundary.OidcApplicationUpdate;
import net.unit8.bouncr.api.boundary.OidcApplicationResponse;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.Presence;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.presenceToNullable;
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
                    LogoutUriPolicy.normalizeBackchannelLogoutUri(presenceToNullable(ok.value().backchannelLogoutUri()));
                    LogoutUriPolicy.normalizeLogoutUri(presenceToNullable(ok.value().frontchannelLogoutUri()));
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
        if (Objects.equals(updateRequest.name().value(), params.get("name"))) {
            return false;
        }
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        return !repo.isNameUnique(updateRequest.name().value());
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
        return OidcApplicationResponse.of(repo.findByName(updateRequest.name().value()).orElseThrow());
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
