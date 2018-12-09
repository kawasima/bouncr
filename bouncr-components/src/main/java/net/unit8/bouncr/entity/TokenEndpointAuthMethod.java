package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TokenEndpointAuthMethod {
    CLIENT_SECRET_POST("POST"),
    CLIENT_SECRET_BASIC("BASIC");

    private final String value;

    TokenEndpointAuthMethod(String value) {
        this.value = value;
    }

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
