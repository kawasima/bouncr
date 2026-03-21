package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;

/**
 * JWKS endpoint — exposes OidcApplication's public key per client_id.
 * GET /oauth2/openid/:client_id/certs
 */
@AllowedMethods("GET")
public class OAuth2JwksResource {
    static final ContextKey<OidcApplication> APP = ContextKey.of(OidcApplication.class);

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
        String kid = RsaJwtSigner.deriveKid(oidcApplication.publicKey());
        Map<String, String> jwk = RsaJwtSigner.publicKeyToJwk(oidcApplication.publicKey(), kid);
        return Map.of("keys", List.of(jwk));
    }
}
