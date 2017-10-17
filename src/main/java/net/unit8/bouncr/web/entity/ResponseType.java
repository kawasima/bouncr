package net.unit8.bouncr.web.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.seasar.doma.Domain;

import java.util.Arrays;

@Domain(valueType = String.class, factoryMethod = "of", accessorMethod = "getName")
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
