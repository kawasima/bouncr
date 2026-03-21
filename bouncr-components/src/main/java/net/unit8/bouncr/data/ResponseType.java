package net.unit8.bouncr.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * OIDC/OAuth2 response types accepted by Bouncr.
 */
public enum ResponseType {
    CODE("code"),
    ID_TOKEN("id_token"),
    TOKEN("token");

    ResponseType(String name) {
        this.name = name;
    }

    private String name;

    @JsonValue
    public String getName() {
        return name;
    }

    /**
     * Parses a response type name in a case-insensitive way.
     *
     * @param name response type text
     * @return matching response type, or {@code null} when unknown
     */
    @JsonCreator
    public static ResponseType of(String name) {
        return Arrays.stream(ResponseType.values())
                .filter(v -> v.name().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }
}
