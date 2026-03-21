package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OAuth2Error;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.AUTHORIZATION_CODE;

/**
 * OAuth2 Token Revocation endpoint (RFC 7009).
 * POST /oauth2/token/revoke (application/x-www-form-urlencoded)
 *
 * <p>Always returns 200 OK regardless of whether the token was valid.
 * Phase 2 implementation revokes authorization codes only.
 * JWT access token revocation (via blacklist) deferred to Phase 3.</p>
 */
@AllowedMethods("POST")
public class OAuth2TokenRevocationResource {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean authorized() {
        return true;
    }

    @Decision(POST)
    public boolean doPost() {
        return true;
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(Parameters params, HttpRequest request, DSLContext dsl) {
        // 1. Authenticate client (required per RFC 7009 §2.1)
        String[] clientCredentials = extractClientCredentials(params, request);
        if (clientCredentials == null) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication required");
        }
        String clientId = clientCredentials[0];
        String clientSecret = clientCredentials[1];

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByClientId(clientId).orElse(null);
        if (app == null) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed");
        }
        byte[] inputHash = PasswordUtils.pbkdf2(clientSecret, clientId, 10000);
        byte[] storedHash = Base64.getDecoder().decode(app.clientSecret());
        if (!MessageDigest.isEqual(inputHash, storedHash)) {
            return tokenError(OAuth2Error.INVALID_CLIENT, "Client authentication failed");
        }

        // 2. Revoke the token (always return 200 per RFC 7009 §2.2)
        String token = params.get("token");
        if (token != null) {
            // Try deleting from authorization code store
            storeProvider.getStore(AUTHORIZATION_CODE).delete(token);
            // Future: also delete from ACCESS_TOKEN blacklist store (Phase 3)
        }

        // 3. Return 200 OK with empty body
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 200)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store"))
                .set(ApiResponse::setBody, Map.of())
                .build();
    }

    private String[] extractClientCredentials(Parameters params, HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)),
                        StandardCharsets.UTF_8);
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) return parts;
            } catch (IllegalArgumentException e) {
                // Invalid Base64
            }
        }
        String id = params.get("client_id");
        String secret = params.get("client_secret");
        if (id != null && secret != null) return new String[]{id, secret};
        return null;
    }

    private ApiResponse tokenError(OAuth2Error error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error.getValue());
        if (description != null) body.put("error_description", description);
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, error.getStatusCode())
                .set(ApiResponse::setHeaders, Headers.of(
                        "Content-Type", "application/json",
                        "Cache-Control", "no-store"))
                .set(ApiResponse::setBody, body)
                .build();
    }
}
