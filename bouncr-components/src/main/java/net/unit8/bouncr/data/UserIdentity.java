package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * Identifies a user by their internal ID and account name — always together.
 *
 * <p>This pair appears repeatedly in the OAuth2 authorization flow:
 * <ul>
 *   <li>{@link AuthorizationCode} — who authorized the grant</li>
 *   <li>{@link OAuth2RefreshToken} — whose session the refresh token represents</li>
 *   <li>JWT claims ({@code uid} + {@code sub}) — who the token was issued for</li>
 * </ul>
 *
 * <p>By modeling this as a single type, we eliminate the risk of passing a
 * {@code userId} from one user with an {@code account} from another.
 *
 * @param userId  the internal user ID (database primary key)
 * @param account the user's account name (used as JWT {@code sub} claim)
 */
public record UserIdentity(long userId, String account) implements Serializable {}
