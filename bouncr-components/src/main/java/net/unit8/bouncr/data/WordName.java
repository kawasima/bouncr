package net.unit8.bouncr.data;

import net.unit8.bouncr.json.WordNameSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import java.util.Locale;

/**
 * A validated name consisting of word characters ({@code \w+}), max 100 characters.
 *
 * <p>Used for account names, group names, role names, realm names,
 * application names, and OIDC provider/application names.
 *
 * @param value the validated name string
 */
@JsonSerialize(using = WordNameSerializer.class)
public record WordName(String value) {
    public String lowercase() {
        return value.toLowerCase(Locale.US);
    }

    public boolean matches(String other) {
        return value.equals(other);
    }
}
