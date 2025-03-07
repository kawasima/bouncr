package net.unit8.bouncr.proxy.health;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

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
        response.setUp(true);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        response.setUp(false);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder status(boolean up) {
        response.setUp(up);
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        return response;
    }
}
