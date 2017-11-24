package net.unit8.bouncr.proxy;

import enkan.exception.MisconfigurationException;
import enkan.middleware.session.KeyValueStore;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.authz.UserPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtHeader;
import net.unit8.bouncr.web.entity.Application;
import net.unit8.bouncr.web.entity.Realm;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;

/**
 * @author kawasima
 */
public class MultiAppProxyClient implements ProxyClient {
    private static final ProxyTarget PROXY_TARGET = new ProxyTarget() {
    };
    private final AttachmentKey<ClientConnection> clientAttachmentKey = AttachmentKey.create(ClientConnection.class);
    private final UndertowClient client;
    private final KeyValueStore store;
    private final RealmCache realmCache;
    private final BouncrConfiguration config;
    private final JsonWebToken jwt;
    private final JwtHeader jwtHeader;

    public MultiAppProxyClient(BouncrConfiguration config, KeyValueStore store, RealmCache realmCache, JsonWebToken jwt) {
        client = UndertowClient.getInstance();
        this.config = config;
        this.store = store;
        this.realmCache = realmCache;
        this.jwt = jwt;
        this.jwtHeader = builder(new JwtHeader())
                .set(JwtHeader::setAlg, "none")
                .build();
    }

    @Override
    public ProxyTarget findTarget(HttpServerExchange exchange) {
        return PROXY_TARGET;
    }


    private String calculatePathTo(String path, Application application) {
        String passTo = application.getUriToPass().getPath();
        if (passTo != null && passTo.endsWith("/")) {
            passTo = passTo.substring(0, passTo.length() - 1);
        }
        return passTo + path.substring(application.getVirtualPath().length(), path.length());
    }

    @Override
    public void getConnection(ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
        Realm realm = realmCache.matches(exchange.getRequestPath());
        if (realm != null) {
            parseToken(exchange).ifPresent(token -> {
                Optional<UserPermissionPrincipal> principal = authenticate(realm.getId(), token);

                principal.ifPresent(p -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("sub", p.getName());
                    body.put("permissions", p.getPermissions());
                    p.getProfiles().forEach((k, v) -> {
                        body.put(k, v);
                    });
                    exchange.getRequestHeaders().put(HttpString.tryFromString(config.getBackendHeaderName()),
                            jwt.sign(body, jwtHeader, null));
                });
            });
        } else {
            exchange.setStatusCode(404);
            exchange.endExchange();
            return;
        }

        Application application = realmCache.getApplication(realm);
        ClientConnection existing = exchange.getConnection().getAttachment(clientAttachmentKey);
        if (existing != null) {
            if (existing.isOpen()) {
                //this connection already has a client, re-use it
                String path = exchange.getRequestPath();
                if (path.startsWith(application.getVirtualPath())) {
                    String passTo = calculatePathTo(path, application);
                    exchange.setRequestPath(passTo);
                    exchange.setRequestURI(passTo);
                }
                callback.completed(exchange, new ProxyConnection(existing, "/"));
                return;
            } else {
                exchange.getConnection().removeAttachment(clientAttachmentKey);
            }
        }

        try {
            URI uri = application.getUriToPass();
            client.connect(new ConnectNotifier(callback, exchange),
                    new URI(uri.getScheme(), /*userInfo*/null, uri.getHost(), uri.getPort(),
                            /*path*/null, /*query*/null, /*fragment*/null),
                    exchange.getIoThread(),
                    exchange.getConnection().getByteBufferPool(), OptionMap.EMPTY);
        } catch (URISyntaxException e) {
            throw new MisconfigurationException("", application.getUriToPass(), e);
        }
    }

    private Optional<String> parseToken(HttpServerExchange exchange) {
        if (exchange.getRequestHeaders().contains("Authorization")) {
            String authorizationValue = exchange.getRequestHeaders().getFirst("Authorization");
            String[] tokens = authorizationValue.split("\\s+");
            if (Objects.equals(tokens[0], "Bearer")) {
                return Optional.of(tokens[1]);
            } else {
                return Optional.empty();
            }
        } else if (exchange.getRequestCookies().containsKey(config.getTokenName())) {
            Cookie tokenCookie = exchange.getRequestCookies().get(config.getTokenName());
            return Optional.of(tokenCookie.getValue());
        } else {
            return Optional.empty();
        }
    }

    private Optional<UserPermissionPrincipal> authenticate(Long realmId, String token) {
        return some((UserPrincipal) store.read(token),
                user -> new UserPermissionPrincipal(user.getId(), user.getName(), user.getProfiles(), user.getPermissions(realmId)));
    }

    private final class ConnectNotifier implements ClientCallback<ClientConnection> {
        private final ProxyCallback<ProxyConnection> callback;
        private final HttpServerExchange exchange;

        private ConnectNotifier(ProxyCallback<ProxyConnection> callback, HttpServerExchange exchange) {
            this.callback = callback;
            this.exchange = exchange;
        }

        @Override
        public void completed(final ClientConnection connection) {
            final ServerConnection serverConnection = exchange.getConnection();
            //we attach to the connection so it can be re-used
            serverConnection.putAttachment(clientAttachmentKey, connection);
            serverConnection.addCloseListener(serverConnection1 -> IoUtils.safeClose(connection));
            connection.getCloseSetter().set((ChannelListener<Channel>) channel -> serverConnection.removeAttachment(clientAttachmentKey));

            exchange.setRelativePath("/");
            Realm realm = realmCache.matches(exchange.getRequestPath());
            Application application = realmCache.getApplication(realm);
            String path = exchange.getRequestPath();
            if (path.startsWith(application.getVirtualPath())) {
                String passTo = calculatePathTo(path, application);
                exchange.setRequestPath(passTo);
                exchange.setRequestURI(passTo);
            }
            callback.completed(exchange, new ProxyConnection(connection, "/"));
        }

        @Override
        public void failed(IOException e) {
            callback.failed(exchange);
        }
    }

}
