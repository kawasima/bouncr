package backendexample;

import io.undertow.Undertow;
import io.undertow.util.Headers;

public class ForTestBackentApp {
    public static void main(String[] args) {
        final Undertow server = Undertow.builder()
                .addHttpListener(8083, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    String user = exchange.getRequestHeaders().getFirst("X-BOUNCR-USER");
                    String permissions = exchange.getRequestHeaders().getFirst("X-BOUNCR-PERMISSIONS");

                    exchange.getResponseSender().send("Server1\n"
                            + "User=" + user + "\n"
                            + "Permissions=" + permissions + "\n"
                    );
                })
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(()->server.stop()));
        server.start();
    }
}
