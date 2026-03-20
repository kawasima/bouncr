package net.unit8.bouncr.json;

import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfileValue;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Jackson serializer for User record.
 * Flattens userProfileValues into top-level fields (e.g. "email", "name")
 * to match the API contract expected by the UI.
 */
public class UserSerializer extends StdSerializer<User> {
    public UserSerializer() {
        super(User.class);
    }

    @Override
    public void serialize(User user, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartObject();
        gen.writeNumberProperty("id", user.id());
        gen.writeStringProperty("account", user.account());

        // Flatten profile values as top-level fields
        if (user.userProfileValues() != null) {
            for (UserProfileValue pv : user.userProfileValues()) {
                if (pv.userProfileField() != null && pv.value() != null) {
                    gen.writeStringProperty(pv.userProfileField().jsonName(), pv.value());
                }
            }
        }

        // lock
        if (user.userLock() != null) {
            gen.writePOJOProperty("lock", user.userLock());
        }

        // groups (if loaded)
        if (user.groups() != null) {
            gen.writePOJOProperty("groups", user.groups());
        }

        // permissions (if loaded)
        if (user.permissions() != null) {
            gen.writePOJOProperty("permissions", user.permissions());
        }

        // oidc_providers (if loaded)
        if (user.oidcUsers() != null) {
            gen.writeName("oidc_providers");
            gen.writeStartArray();
            for (var ou : user.oidcUsers()) {
                if (ou.oidcProvider() != null) {
                    gen.writeString(ou.oidcProvider().name());
                }
            }
            gen.writeEndArray();
        }

        // unverified_profiles (if loaded)
        if (user.unverifiedProfiles() != null && !user.unverifiedProfiles().isEmpty()) {
            gen.writePOJOProperty("unverified_profiles", user.unverifiedProfiles());
        }

        gen.writeEndObject();
    }
}
