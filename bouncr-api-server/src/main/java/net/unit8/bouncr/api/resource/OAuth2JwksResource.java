package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.sign.RsaJwtSigner;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

import static kotowari.restful.DecisionPoint.*;

/**
 * JWKS endpoint — exposes OidcApplication's public key per client_id.
 * GET /oauth2/openid/:client_id/certs
 *
 * Only loads the public key from DB — private_key and client_secret are never selected.
 */
@AllowedMethods("GET")
public class OAuth2JwksResource {
    private static final ContextKey<byte[]> PUBLIC_KEY = ContextKey.of("publicKey", byte[].class);

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        String clientId = params.get("client_id");
        if (clientId == null) return false;
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        return repo.findPublicKeyByClientId(clientId).map(pk -> {
            context.put(PUBLIC_KEY, pk);
            return true;
        }).orElse(false);
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> handleOk(byte[] publicKey) {
        String kid = RsaJwtSigner.deriveKid(publicKey);
        Map<String, String> jwk = RsaJwtSigner.publicKeyToJwk(publicKey, kid);
        return Map.of("keys", List.of(jwk));
    }
}
