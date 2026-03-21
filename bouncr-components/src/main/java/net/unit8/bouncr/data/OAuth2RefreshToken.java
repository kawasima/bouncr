package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * Refresh token data stored in the {@code OAUTH2_REFRESH_TOKEN} KVS store.
 *
 * <p>Created alongside an access token when the authorization code is exchanged.
 * Consumed (once) during refresh token rotation — the old token is deleted and
 * a new one is issued.
 *
 * <p>Fields that belong together are grouped into value objects:
 * <ul>
 *   <li>{@link UserIdentity} — the user this refresh token represents</li>
 *   <li>{@link Scope} — the scope originally granted</li>
 * </ul>
 *
 * @param clientId  the OAuth2 client this token was issued to
 * @param user      the user whose session this token represents
 * @param scope     the scope originally granted
 * @param createdAt epoch seconds when the token was issued
 */
public record OAuth2RefreshToken(
    String clientId,
    UserIdentity user,
    Scope scope,
    long createdAt
) implements Serializable {}
