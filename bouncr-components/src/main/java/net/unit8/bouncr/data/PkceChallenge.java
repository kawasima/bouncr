package net.unit8.bouncr.data;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * PKCE (Proof Key for Code Exchange) challenge — a pair of
 * {@code code_challenge} and {@code code_challenge_method}.
 *
 * <p>These two parameters always appear together in OAuth2 authorization
 * requests. If one is present without the other, the request is malformed.
 * This value object enforces that invariant at construction time and
 * encapsulates the S256 verification logic.
 *
 * <p>Currently only the {@code S256} method is supported. The constructor
 * rejects other methods to prevent downgrade attacks (e.g., {@code plain}).
 *
 * @param challenge the code challenge (Base64url-encoded SHA-256 hash)
 * @param method    the challenge method (must be "S256")
 * @see <a href="https://tools.ietf.org/html/rfc7636">RFC 7636 — PKCE</a>
 */
public record PkceChallenge(String challenge, String method) implements Serializable {

    /**
     * Validates that the challenge is non-blank and the method is S256.
     *
     * @throws IllegalArgumentException if challenge is blank or method is not S256
     */
    public PkceChallenge {
        if (challenge == null || challenge.isBlank()) {
            throw new IllegalArgumentException("code_challenge is required");
        }
        if (!"S256".equals(method)) {
            throw new IllegalArgumentException("Only code_challenge_method=S256 is supported");
        }
    }

    /**
     * Verifies a {@code code_verifier} against this challenge.
     *
     * <p>Computes {@code BASE64URL(SHA256(code_verifier))} and compares it to
     * the stored challenge using constant-time comparison to prevent timing attacks.
     *
     * @param codeVerifier the code verifier from the token request
     * @return {@code true} if the verifier matches this challenge
     */
    public boolean verify(String codeVerifier) {
        if (codeVerifier == null) return false;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    challenge.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
