package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import kotowari.restful.Decision;
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

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> handleOk(Parameters params, DSLContext dsl) {
        String clientId = params.get("client_id");
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByClientId(clientId).orElse(null);
        if (app == null) {
            return Map.of("error", "invalid_client");
        }

        String kid = RsaJwtSigner.deriveKid(app.publicKey());
        Map<String, String> jwk = RsaJwtSigner.publicKeyToJwk(app.publicKey(), kid);
        return Map.of("keys", List.of(jwk));
    }
}
