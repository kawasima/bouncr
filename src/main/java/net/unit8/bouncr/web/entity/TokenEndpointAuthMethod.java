package net.unit8.bouncr.web.entity;

import org.seasar.doma.Domain;

@Domain(valueType = String.class, factoryMethod = "of")
public enum TokenEndpointAuthMethod {
    CLIENT_SECRET_POST("POST"),
    CLIENT_SECRET_BASIC("BASIC");

    private final String value;

    TokenEndpointAuthMethod(String value) {
        this.value = value;
    }

    public static TokenEndpointAuthMethod of(String value) {
        for (TokenEndpointAuthMethod m : TokenEndpointAuthMethod.values()) {
            if (m.value.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException(value);
    }

    public String getValue() {
        return this.value;
    }
}
