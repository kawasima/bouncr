package net.unit8.bouncr.data;

import net.unit8.bouncr.json.PermissionNameSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import java.util.Locale;

/**
 * A validated permission name consisting of word characters and colons ({@code [\w:]+}), max 100 characters.
 *
 * <p>Colons are allowed to support namespaced permissions (e.g., {@code oidc_application:read}).
 *
 * @param value the validated permission name string
 */
@JsonSerialize(using = PermissionNameSerializer.class)
public record PermissionName(String value) {
    public String lowercase() {
        return value.toLowerCase(Locale.US);
    }

    public boolean matches(String other) {
        return value.equals(other);
    }
}
