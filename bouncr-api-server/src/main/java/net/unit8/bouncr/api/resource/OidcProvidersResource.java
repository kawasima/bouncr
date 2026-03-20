package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.OidcProviderCreate;
import net.unit8.bouncr.api.repository.OidcProviderRepository;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class OidcProvidersResource {
    static final ContextKey<OidcProviderCreate> CREATE_REQ = ContextKey.of(OidcProviderCreate.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_PROVIDER_CREATE.decode(body)) {
            case Ok<OidcProviderCreate> ok -> { context.put(CREATE_REQ, ok.value()); yield null; }
            case Err<OidcProviderCreate>(var issues) -> toProblem(issues);
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

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_provider:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(OidcProviderCreate createRequest, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        return !repo.isNameUnique(createRequest.name());
    }

    @Decision(HANDLE_OK)
    public List<OidcProvider> list(Parameters params, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        String q = params.get("q");
        int offset = Optional.ofNullable(params.<String>get("offset")).map(Integer::parseInt).orElse(0);
        int limit = Optional.ofNullable(params.<String>get("limit")).map(Integer::parseInt).orElse(10);
        return repo.search(q, offset, limit);
    }

    @Decision(POST)
    public OidcProvider create(OidcProviderCreate createRequest, DSLContext dsl) {
        OidcProviderRepository repo = new OidcProviderRepository(dsl);
        return repo.insert(
                createRequest.name(),
                createRequest.clientId(),
                createRequest.clientSecret(),
                createRequest.scope(),
                createRequest.responseType(),
                createRequest.tokenEndpoint(),
                createRequest.authorizationEndpoint(),
                createRequest.tokenEndpointAuthMethod(),
                createRequest.redirectUri(),
                createRequest.jwksUri(),
                createRequest.issuer(),
                createRequest.pkceEnabled()
        );
    }
}
