package net.unit8.bouncr.proxy.health;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import static org.eclipse.microprofile.health.HealthCheckResponse.State.DOWN;
import static org.eclipse.microprofile.health.HealthCheckResponse.State.UP;

public class HealthCheckResponseBuilderImpl extends HealthCheckResponseBuilder {
    private HealthCheckResponseImpl response;
    public HealthCheckResponseBuilderImpl() {
        response = new HealthCheckResponseImpl();
    }

    @Override
    public HealthCheckResponseBuilder name(String name) {
        response.setName(name);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, String value) {
        response.addData(key, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, long value) {
        response.addData(key, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, boolean value) {
        response.addData(key, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder up() {
        response.setState(UP);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        response.setState(DOWN);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder state(boolean up) {
        response.setState(up ? UP : DOWN);
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        return response;
    }
}
