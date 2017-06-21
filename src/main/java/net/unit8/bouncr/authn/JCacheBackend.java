package net.unit8.bouncr.authn;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.security.Principal;

/**
 * @author kawasima
 */
public class JCacheBackend implements AuthBackend {
    private Cache<String, Principal> tokenCache;

    public JCacheBackend() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        CacheManager cacheManager = cachingProvider.getCacheManager();

        Configuration<String, Principal> config =
                new MutableConfiguration<String, Principal>()
                        .setTypes(String.class, Principal.class);
        tokenCache = cacheManager.createCache("token", config);
    }
    @Override
    public Principal authenticate(HttpServerExchange exchange) {
        Cookie cookie = exchange.getRequestCookies().get("BOUNCR_TOKEN");
        if (cookie != null) {
            return tokenCache.get(cookie.getValue());
        }
        return null;
    }
}
