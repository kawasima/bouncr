package net.unit8.bouncr.data;

import java.util.Arrays;
import java.util.Optional;

/**
 * OAuth2 grant types supported by the Bouncr Identity Provider.
 *
 * <p>Each enum constant maps to the wire-format string value used in the
 * {@code grant_type} parameter of token requests (RFC 6749 §4).
 *
 * <p>Use {@link #fromString(String)} to parse user input; it returns
 * {@link Optional#empty()} for unrecognized values rather than throwing,
 * allowing the caller to return a proper {@code unsupported_grant_type} error.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4">RFC 6749 §4 — Obtaining Authorization</a>
 */
public enum GrantType {
    /** Authorization Code Grant (RFC 6749 §4.1). */
    AUTHORIZATION_CODE("authorization_code"),
    /** Refresh Token Grant (RFC 6749 §6). */
    REFRESH_TOKEN("refresh_token"),
    /** Client Credentials Grant (RFC 6749 §4.4). */
    CLIENT_CREDENTIALS("client_credentials");

    private final String value;

    GrantType(String value) {
        this.value = value;
    }

    /**
     * Returns the wire-format string (e.g., {@code "authorization_code"}).
     */
    public String getValue() {
        return value;
    }

    /**
     * Parses a grant_type string into the corresponding enum constant.
     *
     * @param s the grant_type parameter value
     * @return the matching constant, or empty if unrecognized
     */
    public static Optional<GrantType> fromString(String s) {
        return Arrays.stream(values())
                .filter(gt -> gt.value.equals(s))
                .findFirst();
    }
}
