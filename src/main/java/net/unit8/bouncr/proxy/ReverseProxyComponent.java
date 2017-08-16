package net.unit8.bouncr.proxy;

import enkan.application.WebApplication;
import enkan.collection.Headers;
import enkan.collection.OptionMap;
import enkan.component.ApplicationComponent;
import enkan.component.ComponentLifecycle;
import enkan.component.DataSourceComponent;
import enkan.component.WebServerComponent;
import enkan.component.doma2.DomaProvider;
import enkan.data.DefaultHttpRequest;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.exception.ServiceUnavailableException;
import enkan.exception.UnreachableException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import org.xnio.streams.ChannelInputStream;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * The component for an reverse proxy server.
 *
 * @author kawasima
 */
public class ReverseProxyComponent extends WebServerComponent {
    private static IoCallback callback = new IoCallback() {
        @Override
        public void onComplete(HttpServerExchange exchange, Sender sender) {

        }

        @Override
        public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {

        }
    };

    @DecimalMin("1")
    @DecimalMax("65535")
    private Integer port;

    private String host;

    private int ioThreads = 4;

    private int maxRequestTime = 30000;

    private boolean rewriteHostHeader = false;

    private boolean reuseXForwarded = true;

    private Undertow server;

    @Override
    protected ComponentLifecycle<ReverseProxyComponent> lifecycle() {
        return new ComponentLifecycle<ReverseProxyComponent>() {
            @Override
            public void start(ReverseProxyComponent component) {
                StoreProvider storeProvider = getDependency(StoreProvider.class);
                RealmCache realmCache = getDependency(RealmCache.class);
                ApplicationComponent app = getDependency(ApplicationComponent.class);

                if (server == null) {
                    OptionMap options = OptionMap.of("join?", false);
                    if (port != null) options.put("port", port);
                    if (host != null) options.put("host", host);
                    HttpHandler appHandler = createAdminApp((WebApplication) app.getApplication());
                    Map<String, LocationConfig> config = new HashMap<>();
                    MultiAppProxyClient proxyClient = new MultiAppProxyClient(storeProvider.getStore(), realmCache, config);
                    ProxyHandler proxyHandler = new ProxyHandler(proxyClient, maxRequestTime, ResponseCodeHandler.HANDLE_404, rewriteHostHeader, reuseXForwarded);
                    server = Undertow.builder()
                            .addHttpListener(port, host)
                            .setHandler(Handlers.path()
                                    .addPrefixPath("/my", appHandler)
                                    .addPrefixPath("/admin", appHandler)
                                    .addPrefixPath("/", proxyHandler))
                            .build();
                    server.start();
                }

            }

            @Override
            public void stop(ReverseProxyComponent component) {
                if (server != null) {
                    server.stop();
                    server = null;
                }
            }
        };
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setMaxRequestTime(int maxRequestTime) {
        this.maxRequestTime = maxRequestTime;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    @Override
    public int getPort() {
        return port;
    }


    private HttpHandler createAdminApp(WebApplication application) {
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                }
                HttpRequest request = new DefaultHttpRequest();
                request.setRequestMethod(exchange.getRequestMethod().toString());
                request.setUri(exchange.getRequestURI());
                request.setProtocol(exchange.getProtocol().toString());
                request.setQueryString(exchange.getQueryString());
                request.setCharacterEncoding(exchange.getRequestCharset());
                request.setBody(new ChannelInputStream(exchange.getRequestChannel()));
                request.setContentLength(exchange.getRequestContentLength());
                request.setRemoteAddr(exchange.getSourceAddress().toString());
                request.setScheme(exchange.getRequestScheme());
                request.setServerName(exchange.getHostName());
                request.setServerPort(exchange.getHostPort());
                Headers headers = Headers.empty();
                exchange.getRequestHeaders().forEach(e -> {
                    String headerName = e.getHeaderName().toString();
                    e.forEach(v -> headers.put(headerName, v));
                });
                request.setHeaders(headers);

                try {
                    HttpResponse response = application.handle(request);
                    exchange.setStatusCode(response.getStatus());
                    setResponseHeaders(response.getHeaders(), exchange);

                    exchange.startBlocking();
                    setBody(exchange.getResponseSender(), response.getBody());
                } catch (ServiceUnavailableException ex) {
                    exchange.setStatusCode(503);
                } finally {
                    exchange.endExchange();
                }
            }
        };
    }

    private static void setBody(Sender sender, Object body) throws IOException {
        if (body == null) {
            return; // Do nothing
        }

        if (body instanceof String) {
            sender.send((String) body);
        } else if (body instanceof InputStream) {
            ReadableByteChannel chan = Channels.newChannel((InputStream) body);

            ByteBuffer buf = ByteBuffer.allocate(4096);
            for (;;) {
                int size = chan.read(buf);
                if (size <= 0) break;
                buf.flip();
                sender.send(buf, callback);
                buf.clear();
            }
            sender.close(IoCallback.END_EXCHANGE);
        } else if (body instanceof File) {
            try(FileInputStream fis = new FileInputStream((File) body);
                FileChannel chan = fis.getChannel()) {
                ByteBuffer buf = ByteBuffer.allocate(4096);
                for (;;) {
                    int size = chan.read(buf);
                    if (size <= 0) break;
                    buf.flip();
                    sender.send(buf, callback);
                    buf.clear();
                }
                sender.close(IoCallback.END_EXCHANGE);
            }
        } else {
            throw new UnreachableException();
        }
    }

    private void setResponseHeaders(Headers headers, HttpServerExchange exchange) {
        HeaderMap map = exchange.getResponseHeaders();
        headers.keySet().forEach(headerName -> headers.getList(headerName)
                .forEach(v -> {
                    if (v instanceof String) {
                        map.add(HttpString.tryFromString(headerName), (String) v);
                    } else if (v instanceof Number) {
                        map.add(HttpString.tryFromString(headerName), ((Number) v).longValue());
                    }
                }));
    }


}
