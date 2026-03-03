package net.unit8.bouncr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class JacksonTest {

    @Test
    public void test() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        User u = mapper.readValue("{\"name\":\"kawasima\", \"email\":\"hoge\"}", User.class);
        System.out.println(u);
    }

    public static class User {
        private String name;
        private Map<String, Object> additionalProperties = new HashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperties(String name, Object value) {
            additionalProperties.put(name, value);
        }

        @Override
        public String toString() {
            JsonMapper mapper = JsonMapper.builder().build();
            return mapper.writeValueAsString(this);
        }
    }
}
