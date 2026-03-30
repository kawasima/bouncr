package net.unit8.bouncr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import net.unit8.bouncr.data.WordName;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonTest {

    @Test
    public void wordName_serializesAsString() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        String json = mapper.writeValueAsString(new WordName("hello"));
        assertThat(json).isEqualTo("\"hello\"");
    }

    @Test
    public void test() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        User u = mapper.readValue("{\"name\":\"kawasima\", \"email\":\"hoge\"}", User.class);
        assertThat(u.getName()).isEqualTo("kawasima");
        assertThat(u.getAdditionalProperties()).containsEntry("email", "hoge");
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
    }
}
