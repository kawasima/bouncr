package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.OidcProviderRepository;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.bouncr.data.OidcProviderClientConfig;
import net.unit8.bouncr.data.OidcProviderMetadata;
import net.unit8.bouncr.data.WordName;
import net.unit8.bouncr.api.util.ContextKeys;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.decode.combinator.Tuple3;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OidcProviderResource {
    static final ContextKey<Tuple3<WordName, OidcProviderMetadata, OidcProviderClientConfig>> UPDATE_REQ =
            ContextKeys.of(Tuple3.class);
    static final ContextKey<OidcProvider> OIDC_PROVIDER = ContextKey.of(OidcProvider.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_PROVIDER_UPDATE.decode(body)) {
            case Ok(Tuple3(var name, var meta, var clientCfg)) -> {
                context.put(UPDATE_REQ, new Tuple3<>((WordName) name, (OidcProviderMetadata) meta, (OidcProviderClientConfig) clientCfg));
                yield null;
            }
            case Err(var issues) -> toProblem(issues);
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
    public boolean isConflict(Tuple3<WordName, OidcProviderMetadata, OidcProviderClientConfig> updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest._1().value(), params.get("name"))) {
            return false;
        }
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        return !repo.isNameUnique(updateRequest._1().value());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        Optional<OidcProvider> oidcProvider = repo.findByName(params.get("name"));
        oidcProvider.ifPresent(p -> context.put(OIDC_PROVIDER, p));
        return oidcProvider.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(OidcProvider oidcProvider) {
        return BouncrJsonEncoders.OIDC_PROVIDER.encode(oidcProvider);
    }

    @Decision(PUT)
    public Map<String, Object> update(Tuple3<WordName, OidcProviderMetadata, OidcProviderClientConfig> updateRequest, OidcProvider oidcProvider, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        var meta = updateRequest._2();
        var clientCfg = updateRequest._3();
        repo.update(
                oidcProvider.name(),
                updateRequest._1().value(),
                clientCfg.credentials().clientId(),
                clientCfg.credentials().clientSecret(),
                clientCfg.scope(),
                clientCfg.responseType() != null ? clientCfg.responseType().getName() : null,
                meta.tokenEndpoint(),
                meta.authorizationEndpoint(),
                clientCfg.tokenEndpointAuthMethod() != null ? clientCfg.tokenEndpointAuthMethod().getValue() : null,
                clientCfg.redirectUri() != null ? clientCfg.redirectUri().toString() : null,
                meta.jwksUri() != null ? meta.jwksUri().toString() : null,
                meta.issuer(),
                clientCfg.pkceEnabled()
        );
        actionRecord.setActionType(ActionType.OIDC_PROVIDER_MODIFIED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(oidcProvider.name());
        return BouncrJsonEncoders.OIDC_PROVIDER.encode(repo.findByName(updateRequest._1().value()).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(OidcProvider oidcProvider, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        repo.delete(oidcProvider.name());
        actionRecord.setActionType(ActionType.OIDC_PROVIDER_DELETED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(oidcProvider.name());
        return null;
    }
}
