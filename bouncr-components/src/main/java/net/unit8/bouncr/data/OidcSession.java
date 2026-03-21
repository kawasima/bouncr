package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * OIDC authorization request state stored between redirects.
 *
 * @param nonce nonce used for ID token replay protection
 * @param state CSRF protection value
 * @param responseType response type used in the original authorization request
 * @param codeVerifier PKCE code verifier (nullable when PKCE is not used)
 */
public record OidcSession(
    String nonce,
    String state,
    ResponseType responseType,
    String codeVerifier
) implements Serializable {
}
