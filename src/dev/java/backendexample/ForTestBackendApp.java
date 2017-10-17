package backendexample;

import io.undertow.Undertow;
import io.undertow.util.Headers;
import net.unit8.bouncr.sign.JsonWebToken;

import java.util.Base64;

public class ForTestBackendApp {
    public static void main(String[] args) {
        Base64.Decoder b64decoder = Base64.getUrlDecoder();
        final Undertow server = Undertow.builder()
                .addHttpListener(8083, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    String credential = exchange.getRequestHeaders().getFirst("X-Bouncr-Credential");
                    String[] tokens = credential.split("\\.", 3);
                    String json = new String(b64decoder.decode(tokens[1]));


                    exchange.getResponseSender().send("Server1\n"
                            + "profile=" + json + "\n"
                    );
                })
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
