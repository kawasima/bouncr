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
import enkan.exception.MisconfigurationException;
import enkan.exception.ServiceUnavailableException;
import enkan.exception.UnreachableException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.ClientCertAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import org.xnio.streams.ChannelInputStream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

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

    @DecimalMin("1")
    @DecimalMax("65535")
    private Integer sslPort;
    private String keystore;
    private String keyPassword;
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
                    MultiAppProxyClient proxyClient = new MultiAppProxyClient(storeProvider.getStore(), realmCache);
                    ProxyHandler proxyHandler = new ProxyHandler(proxyClient, maxRequestTime, ResponseCodeHandler.HANDLE_404, rewriteHostHeader, reuseXForwarded);

                    IdentityManager identityManager = new IdentityManager() {
                        @Override
                        public Account verify(Account account) {
                            return account;
                        }

                        @Override
                        public Account verify(String id, Credential credential) {
                            return null;
                        }

                        @Override
                        public Account verify(Credential credential) {
                            return null;
                        }
                    };

                    Undertow.Builder builder = Undertow.builder()
                            .addHttpListener(port, host)
                            .setHandler(addSecurity(
                                    Handlers.path()
                                            .addPrefixPath("/my", appHandler)
                                            .addPrefixPath("/admin", appHandler)
                                            .addPrefixPath("/", proxyHandler)
                                    , identityManager)
                            );
                    if (sslPort != null) {
                        builder.addHttpsListener(sslPort, host, createSSLContext());
                    }
                    server = builder.build();
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

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
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

    private KeyManager[] getKeyManagers() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keystore),
                keyPassword.toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "password".toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    private SSLContext createSSLContext()  {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(getKeyManagers(), null, null);
            return context;
        } catch (NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException | CertificateException | KeyStoreException | IOException e) {
            // TODO
            throw new MisconfigurationException("bouncr.", e);
        }
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

    private static HttpHandler addSecurity(final HttpHandler toWrap, final IdentityManager identityManager) {
        HttpHandler handler = toWrap;
        handler = new AuthenticationCallHandler(handler);
///        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.singletonList(new ClientCertAuthenticationMechanism("My Realm"));
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
        return handler;
    }
}
