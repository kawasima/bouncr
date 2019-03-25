package net.unit8.bouncr.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.eclipse.microprofile.health.HealthCheckResponse;

public class HealthCheckHandler implements HttpHandler {
    private final ObjectMapper mapper;

    public HealthCheckHandler() {
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();
        HealthCheckResponse response = HealthCheckResponse.builder()
                .name("bouncr-proxy")
                .up()
                .build();
        exchange.setStatusCode(200);
        exchange.getResponseSender().send(mapper.writeValueAsString(response));
    }
}
