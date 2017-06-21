package net.unit8.bouncr.proxy;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kawasima
 */
public class CertProxy {
    private Undertow undertow;
    private int port = 3128;
    private int ioThreads = 4;
    private int maxRequestTime = 30000;

    public static void main(String[] args) throws Exception {
        new CertProxy().start();
    }

    public void start() throws Exception {
        boolean rewriteHostHeader = false;
        boolean reuseXForwarded = true;

        Map<String, LocationConfig> config = new HashMap<>();
        MultiAppProxyClient proxyClient = new MultiAppProxyClient(config);
        ProxyHandler proxyHandler = new ProxyHandler(proxyClient, maxRequestTime, ResponseCodeHandler.HANDLE_404, rewriteHostHeader, reuseXForwarded);
        HttpHandler handler = Handlers.path()
                .addPrefixPath("/", proxyHandler);

        Undertow.Builder proxyBuilder = Undertow.builder()
                .setIoThreads(ioThreads)
                .setHandler(handler);

        proxyBuilder.addHttpListener(port, "localhost");
        this.undertow = proxyBuilder.build();
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }
}
