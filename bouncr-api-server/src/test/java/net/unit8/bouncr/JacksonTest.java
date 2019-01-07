package net.unit8.bouncr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class JacksonTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
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
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(this);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
