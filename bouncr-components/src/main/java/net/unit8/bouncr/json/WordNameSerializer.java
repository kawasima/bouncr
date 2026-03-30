package net.unit8.bouncr.json;

import net.unit8.bouncr.data.WordName;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class WordNameSerializer extends StdSerializer<WordName> {
    public WordNameSerializer() {
        super(WordName.class);
    }

    @Override
    public void serialize(WordName wordName, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeString(wordName.value());
    }
}
