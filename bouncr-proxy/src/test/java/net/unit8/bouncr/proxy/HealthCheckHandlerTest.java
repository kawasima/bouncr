package net.unit8.bouncr.proxy;

import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.*;

public class HealthCheckHandlerTest {
    @Test
    void healthCheck_Normal() throws Exception {
        HttpServerExchange exchange = mock(HttpServerExchange.class);
        Sender sender = mock(Sender.class);
        given(exchange.getResponseSender()).willReturn(sender);
        HealthCheckHandler handler = new HealthCheckHandler();
        handler.handleRequest(exchange);
        verify(sender).send(anyString());
    }
}
