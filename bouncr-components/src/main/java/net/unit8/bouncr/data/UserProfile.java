package net.unit8.bouncr.data;

import java.util.Map;
import java.util.Optional;

/**
 * Validated user profile containing field values keyed by JSON name.
 * Built by a dynamic decoder that validates against DB-loaded field definitions.
 */
public record UserProfile(Map<String, String> values) {
    public Optional<String> get(String jsonName) {
        return Optional.ofNullable(values.get(jsonName));
    }
}
