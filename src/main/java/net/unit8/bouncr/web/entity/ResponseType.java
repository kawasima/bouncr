package net.unit8.bouncr.web.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

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

    @JsonCreator
    public static ResponseType of(String name) {
        return Arrays.stream(ResponseType.values())
                .filter(v -> v.name().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }
}
