package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.middleware.session.JCacheStore;
import enkan.middleware.session.KeyValueStore;

/**
 * @author kawasima
 */
public class StoreProvider extends SystemComponent {
    private KeyValueStore store;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<StoreProvider>() {
            @Override
            public void start(StoreProvider provider) {
                provider.store = new JCacheStore();
            }

            @Override
            public void stop(StoreProvider provider) {
                provider.store = null;
            }
        };
    }

    public KeyValueStore getStore() {
        return store;
    }
}
