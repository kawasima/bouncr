package net.unit8.bouncr.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import net.unit8.bouncr.component.RealmCache;

public class CacheRefreshHandler implements HttpHandler {
    private final RealmCache realmCache;
    public CacheRefreshHandler(RealmCache realmCache) {
        this.realmCache = realmCache;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        realmCache.refresh();
        exchange.setStatusCode(204);
        exchange.getResponseSender().send("");

    }


}
