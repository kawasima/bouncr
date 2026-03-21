package net.unit8.bouncr.api.service;

import enkan.collection.Parameters;
import enkan.data.HttpRequest;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Shared OAuth2 client authentication logic for token and revocation endpoints.
 * Supports HTTP Basic (client_secret_basic) and POST body (client_secret_post).
 */
public class OAuth2ClientAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(OAuth2ClientAuthenticator.class);

    private final BouncrConfiguration config;

    public OAuth2ClientAuthenticator(BouncrConfiguration config) {
        this.config = config;
    }

    /**
     * Result of client authentication.
     */
    public record AuthResult(OidcApplication app, boolean basicAuthAttempted) {}

    /**
     * Authenticate the OAuth2 client from the request.
     * Returns the OidcApplication if authentication succeeds, or null if it fails.
     */
    public AuthResult authenticate(Parameters params, HttpRequest request, DSLContext dsl) {
        boolean basicAuthAttempted = hasBasicAuth(request);
        String[] credentials = extractCredentials(params, request);
        if (credentials == null) {
            return null;
        }
        String clientId = credentials[0];
        String clientSecret = credentials[1];

        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        OidcApplication app = repo.findByClientId(clientId).orElse(null);
        if (app == null) {
            return null;
        }

        byte[] inputHash = PasswordUtils.pbkdf2(clientSecret, clientId, config.getPbkdf2Iterations());
        byte[] storedHash;
        try {
            storedHash = Base64.getDecoder().decode(app.clientSecret());
        } catch (IllegalArgumentException e) {
            storedHash = null;
        }

        // If decoded length matches PBKDF2 output, compare as hashed secret
        if (storedHash != null && storedHash.length == inputHash.length) {
            if (!MessageDigest.isEqual(inputHash, storedHash)) {
                return null;
            }
        } else {
            // Legacy plaintext secret — compare directly
            LOG.warn("Client {} using legacy plaintext secret — migrate to PBKDF2 hash", clientId);
            if (!MessageDigest.isEqual(
                    clientSecret.getBytes(StandardCharsets.UTF_8),
                    app.clientSecret().getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
        }

        return new AuthResult(app, basicAuthAttempted);
    }

    public boolean hasBasicAuth(HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        return authHeader != null && authHeader.startsWith("Basic ");
    }

    private String[] extractCredentials(Parameters params, HttpRequest request) {
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
}
