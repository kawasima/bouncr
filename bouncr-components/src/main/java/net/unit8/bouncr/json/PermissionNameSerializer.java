package net.unit8.bouncr.json;

import net.unit8.bouncr.data.PermissionName;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class PermissionNameSerializer extends StdSerializer<PermissionName> {
    public PermissionNameSerializer() {
        super(PermissionName.class);
    }

    @Override
    public void serialize(PermissionName permissionName, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeString(permissionName.value());
    }
}
