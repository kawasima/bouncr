package net.unit8.bouncr.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Client authentication method at the OAuth2 token endpoint.
 */
public enum TokenEndpointAuthMethod {
    /** Client credentials passed in POST parameters (RFC 7591: client_secret_post). */
    CLIENT_SECRET_POST("client_secret_post"),
    /** Client credentials passed via HTTP Basic authentication (RFC 7591: client_secret_basic). */
    CLIENT_SECRET_BASIC("client_secret_basic");

    private final String value;

    TokenEndpointAuthMethod(String value) {
        this.value = value;
    }

    /**
     * Parses the persisted/authenticated representation.
     *
     * @param value serialized value
     * @return matching authentication method
     * @throws IllegalArgumentException when the value is unsupported
     */
    @JsonCreator
    public static TokenEndpointAuthMethod of(String value) {
        for (TokenEndpointAuthMethod m : TokenEndpointAuthMethod.values()) {
            if (m.value.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException(value);
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }
}
