package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * Authorization code data stored in the {@code AUTHORIZATION_CODE} KVS store.
 *
 * <p>Created when the authorize endpoint issues a code, consumed (once) when the
 * token endpoint exchanges it for tokens. The store TTL provides the primary
 * expiry mechanism; {@code createdAt} allows a defense-in-depth server-side check.
 *
 * <p>Fields that belong together are grouped into value objects:
 * <ul>
 *   <li>{@link UserIdentity} — the user who authorized the grant</li>
 *   <li>{@link Scope} — the requested scopes</li>
 *   <li>{@link PkceChallenge} — the PKCE challenge (nullable if PKCE not used)</li>
 * </ul>
 *
 * @param clientId    the OAuth2 client that requested authorization
 * @param user        the user who authorized the grant
 * @param scope       the authorized scopes
 * @param nonce       OIDC nonce for replay protection (nullable)
 * @param pkce        PKCE challenge to verify at token exchange (nullable)
 * @param redirectUri the redirect URI from the authorization request
 * @param createdAt   epoch seconds when the code was issued
 */
public record AuthorizationCode(
    String clientId,
    UserIdentity user,
    Scope scope,
    String nonce,
    PkceChallenge pkce,
    String redirectUri,
    long createdAt
) implements Serializable {}
