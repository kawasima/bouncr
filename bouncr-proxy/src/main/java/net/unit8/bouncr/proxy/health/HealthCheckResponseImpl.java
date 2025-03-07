package net.unit8.bouncr.proxy.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HealthCheckResponseImpl extends HealthCheckResponse {
    private String name;
    private Map<String, Object> data;
    private boolean up;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Status getStatus() {
        return up ? Status.UP : Status.DOWN;
    }

    public void setUp(boolean up) {
        this.up = up;
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
