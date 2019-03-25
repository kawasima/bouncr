package net.unit8.bouncr.proxy.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class HealthCheckResponseImpl extends HealthCheckResponse {
    private String name;
    private Map<String, Object> data;
    private State state;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public Optional<Map<String, Object>> getData() {
        return Optional.ofNullable(data);
    }

    public void addData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }
}
