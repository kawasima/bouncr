package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.middleware.session.KeyValueStore;
import net.unit8.bouncr.component.config.KvsSettings;

/**
 * @author kawasima
 */
public class StoreProvider extends SystemComponent<StoreProvider> {
    private KeyValueStore bouncrTokenStore;
    private KeyValueStore authorizationCodeStore;
    private KeyValueStore accessTokenStore;
    private KeyValueStore oidcSessionStore;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<StoreProvider>() {
            @Override
            public void start(StoreProvider provider) {
                BouncrConfiguration config = getDependency(BouncrConfiguration.class);
                KvsSettings settings = config.getKeyValueStoreSettings();
                provider.bouncrTokenStore = settings
                        .getBouncrTokenStoreFactory()
                        .apply(getAllDependencies());

                provider.authorizationCodeStore = settings
                        .getAuthorizationCodeStoreFactory()
                        .apply(getAllDependencies());

                provider.accessTokenStore = settings
                        .getAccessTokenStoreFactory()
                        .apply(getAllDependencies());

                provider.oidcSessionStore = settings
                        .getOidcSessionStoreFactory()
                        .apply(getAllDependencies());
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
            case OIDC_SESSION: return oidcSessionStore;
            default: throw new IllegalArgumentException(storeType + " is unknown");
        }
    }

    public enum StoreType {
        BOUNCR_TOKEN,
        AUTHORIZATION_CODE,
        ACCESS_TOKEN,
        OIDC_SESSION
    }
}
