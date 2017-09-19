package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.middleware.session.JCacheStore;
import enkan.middleware.session.KeyValueStore;

import javax.cache.configuration.Factory;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.util.concurrent.TimeUnit;

/**
 * @author kawasima
 */
public class StoreProvider extends SystemComponent {
    private KeyValueStore bouncrTokenStore;
    private KeyValueStore authorizationCodeStore;
    private KeyValueStore accessTokenStore;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<StoreProvider>() {
            @Override
            public void start(StoreProvider provider) {
                BouncrConfiguration config = getDependency(BouncrConfiguration.class);
                Factory<ExpiryPolicy> expiryPolicyFactory = AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, config.getTokenExpires()));
                provider.bouncrTokenStore = new JCacheStore(expiryPolicyFactory);

                provider.authorizationCodeStore = new JCacheStore(
                        AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, config.getAuthorizationCodeExpires()))
                );
                provider.accessTokenStore = new JCacheStore();
            }

            @Override
            public void stop(StoreProvider provider) {
                provider.bouncrTokenStore = null;
            }
        };
    }

    public KeyValueStore getStore(StoreType storeType) {
        switch(storeType) {
            case BOUNCR_TOKEN: return bouncrTokenStore;
            case AUTHORIZATION_CODE: return authorizationCodeStore;
            case ACCESS_TOKEN: return accessTokenStore;
            default: throw new IllegalArgumentException(storeType + " is unknown");
        }
    }

    public enum StoreType {
        BOUNCR_TOKEN,
        AUTHORIZATION_CODE,
        ACCESS_TOKEN
    }
}
