package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;

/**
 * OpenID Connect Discovery endpoint (per client_id).
 * GET /oauth2/openid/:client_id/.well-known/openid-configuration
 */
@AllowedMethods("GET")
public class OAuth2DiscoveryResource {
    static final ContextKey<OidcApplication> APP = ContextKey.of(OidcApplication.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        String clientId = params.get("client_id");
        if (clientId == null) return false;
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        return repo.findByClientId(clientId).map(app -> {
            context.put(APP, app);
            return true;
        }).orElse(false);
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> handleOk(OidcApplication oidcApplication) {
        String baseUrl = config.getIssuerBaseUrl();
        String issuer = baseUrl + "/oauth2/openid/" + oidcApplication.clientId();

        return Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", baseUrl + "/oauth2/authorize"),
                Map.entry("token_endpoint", baseUrl + "/oauth2/token"),
                Map.entry("userinfo_endpoint", baseUrl + "/oauth2/userinfo"),
                Map.entry("revocation_endpoint", baseUrl + "/oauth2/token/revoke"),
                Map.entry("introspection_endpoint", baseUrl + "/oauth2/token/introspect"),
                Map.entry("jwks_uri", issuer + "/certs"),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported",
                        oidcApplication.grantTypes() != null
                                ? oidcApplication.grantTypes().stream().map(GrantType::getValue).toList()
                                : GrantType.DEFAULT_GRANT_TYPES),
                Map.entry("subject_types_supported", List.of("public")),
                Map.entry("id_token_signing_alg_values_supported", List.of("RS256")),
                Map.entry("scopes_supported", List.of("openid", "profile", "email")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post")),
                Map.entry("claims_supported", List.of("sub", "iss", "aud", "exp", "iat", "nonce", "name", "email")),
                Map.entry("code_challenge_methods_supported", List.of("S256"))
        );
    }
}
