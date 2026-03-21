package net.unit8.bouncr.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents an OAuth2/OIDC scope as an immutable set of scope tokens.
 *
 * <p>In the OAuth2 protocol, scopes are communicated as a single space-delimited
 * string (e.g., {@code "openid profile email"}). This value object parses that
 * string into a set of individual tokens and provides set-theoretic operations
 * needed for scope validation throughout the authorization flow:
 *
 * <ul>
 *   <li>{@link #contains(String)} — check if a specific scope token is present
 *       (e.g., {@code scope.contains("openid")})</li>
 *   <li>{@link #isSubsetOf(Scope)} — enforce that a refresh token request does not
 *       escalate scope beyond the original grant (RFC 6749 §6)</li>
 * </ul>
 *
 * <p>Used in {@link AuthorizationCode}, {@link OAuth2RefreshToken}, and throughout
 * the token endpoint for scope validation.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3">RFC 6749 §3.3 — Access Token Scope</a>
 */
public record Scope(Set<String> values) implements Serializable {

    /**
     * Defensive copy into a sorted, unmodifiable set.
     * TreeSet ensures deterministic iteration order for {@link #toString()}.
     */
    public Scope {
        values = Collections.unmodifiableSet(new TreeSet<>(values));
    }

    /**
     * Parses a space-delimited scope string into a {@code Scope} instance.
     *
     * <p>Duplicate tokens are silently de-duplicated (unlike {@code Set.of()} which
     * throws on duplicates). Blank input produces an empty scope.
     *
     * @param scopeString the space-delimited scope string, or null
     * @return a {@code Scope} containing the parsed tokens
     */
    public static Scope parse(String scopeString) {
        if (scopeString == null || scopeString.isBlank()) {
            return new Scope(Set.of());
        }
        return new Scope(new LinkedHashSet<>(Arrays.asList(scopeString.trim().split("\\s+"))));
    }

    /**
     * Returns {@code true} if this scope contains the given token.
     *
     * @param scope the scope token to check for
     * @return true if present
     */
    public boolean contains(String scope) {
        return values.contains(scope);
    }

    /**
     * Returns {@code true} if all tokens in this scope are also present in {@code other}.
     *
     * <p>Used when validating refresh token requests — the requested scope
     * must not exceed the scope originally granted (RFC 6749 §6).
     *
     * @param other the scope to check against
     * @return true if this is a subset of other
     */
    public boolean isSubsetOf(Scope other) {
        return other.values.containsAll(this.values);
    }

    /**
     * Returns the space-delimited string representation, suitable for OAuth2 responses.
     */
    @Override
    public String toString() {
        return String.join(" ", values);
    }
}
