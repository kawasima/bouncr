package net.unit8.bouncr.api;

import enkan.component.ComponentLifecycle;
import enkan.component.jedis.JedisProvider;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.AuthorizationCode;
import net.unit8.bouncr.data.OAuth2RefreshToken;
import net.unit8.bouncr.data.OidcSession;

import java.util.HashMap;

/**
 * Production StoreProvider that creates Redis-backed stores with TTLs
 * sourced from {@link BouncrConfiguration}.
 *
 * <p>Requires {@link JedisProvider} as a dependency (declared via
 * {@code component("storeprovider").using("config", "redis")}).</p>
 */
public class RedisStoreProvider extends StoreProvider {
    @Override
    protected ComponentLifecycle<StoreProvider> lifecycle() {
        return new ComponentLifecycle<>() {
            @Override
            public void start(StoreProvider provider) {
                BouncrConfiguration config = getDependency(BouncrConfiguration.class);
                JedisProvider redis = getDependency(JedisProvider.class);

                provider.setBouncrTokenStore(redis.createStore(
                        "BOUNCR_TOKEN", HashMap.class, config.getAccessTokenExpires()));
                provider.setRefreshTokenStore(redis.createStore(
                        "BOUNCR_REFRESH", HashMap.class, config.getRefreshTokenExpires()));
                provider.setAuthorizationCodeStore(redis.createStore(
                        "BOUNCR_AUTHORIZATION_CODE", AuthorizationCode.class, config.getAuthorizationCodeExpires()));
                provider.setAccessTokenStore(redis.createStore(
                        "BOUNCR_ACCESS_TOKEN", OAuth2RefreshToken.class, config.getOauth2RefreshTokenExpires()));
                provider.setOidcSessionStore(redis.createStore(
                        "BOUNCR_OIDC_SESSION", OidcSession.class, config.getOidcSessionExpires()));
            }

            @Override
            public void stop(StoreProvider provider) {
                // Do not null out stores — let GC handle cleanup.
            }
        };
    }
}
