package net.unit8.bouncr.data;

/**
 * OAuth2 authorization request parameters (RFC 6749 §4.1.1).
 *
 * @param responseType must be "code"
 * @param clientId     the requesting client's identifier
 * @param redirectUri  must match the registered callback URI
 * @param scope        requested scopes
 * @param state        opaque state for CSRF protection
 * @param nonce        for ID token replay protection
 * @param pkce         PKCE challenge ({@code null} if not using PKCE)
 */
public record AuthorizeRequest(
        String responseType,
        String clientId,
        String redirectUri,
        Scope scope,
        String state,
        String nonce,
        PkceChallenge pkce
) {}
