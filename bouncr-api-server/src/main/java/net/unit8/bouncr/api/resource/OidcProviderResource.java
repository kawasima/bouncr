package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.OidcProviderUpdate;
import net.unit8.bouncr.api.repository.OidcProviderRepository;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcProviderResource {
    static final ContextKey<OidcProviderUpdate> UPDATE_REQ = ContextKey.of(OidcProviderUpdate.class);
    static final ContextKey<OidcProvider> OIDC_PROVIDER = ContextKey.of(OidcProvider.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_PROVIDER_UPDATE.decode(body)) {
            case Ok<OidcProviderUpdate> ok -> { context.put(UPDATE_REQ, ok.value()); yield null; }
            case Err<OidcProviderUpdate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(OidcProviderUpdate updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest.name(), params.get("name"))) {
            return false;
        }
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        return !repo.isNameUnique(updateRequest.name());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        Optional<OidcProvider> oidcProvider = repo.findByName(params.get("name"));
        oidcProvider.ifPresent(p -> context.put(OIDC_PROVIDER, p));
        return oidcProvider.isPresent();
    }

    @Decision(HANDLE_OK)
    public OidcProvider find(OidcProvider oidcProvider) {
        return oidcProvider;
    }

    @Decision(PUT)
    public OidcProvider update(OidcProviderUpdate updateRequest, OidcProvider oidcProvider, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        repo.update(
                oidcProvider.name(),
                updateRequest.name(),
                updateRequest.clientId(),
                updateRequest.clientSecret(),
                updateRequest.scope(),
                updateRequest.responseType(),
                updateRequest.tokenEndpoint(),
                updateRequest.authorizationEndpoint(),
                updateRequest.tokenEndpointAuthMethod(),
                updateRequest.redirectUri(),
                updateRequest.jwksUri(),
                updateRequest.issuer(),
                updateRequest.pkceEnabled()
        );
        return repo.findByName(updateRequest.name()).orElseThrow();
    }

    @Decision(DELETE)
    public Void delete(OidcProvider oidcProvider, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        repo.delete(oidcProvider.name());
        return null;
    }
}
