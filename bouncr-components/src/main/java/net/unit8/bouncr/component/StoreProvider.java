package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.middleware.session.KeyValueStore;
import enkan.middleware.session.MemoryStore;

/**
 * Provides KeyValueStore instances for token and session storage.
 *
 * <p>Store instances are injected via setters before the component starts.
 * In production, {@code BouncrApiEnkanSystemFactory} creates Redis-backed stores
 * using JedisProvider. In development, {@code BouncrDevEnkanSystemFactory} uses
 * MemoryStore (the default).</p>
 *
 * @author kawasima
 */
public class StoreProvider extends SystemComponent<StoreProvider> {
    private KeyValueStore bouncrTokenStore = new MemoryStore();
    private KeyValueStore refreshTokenStore = new MemoryStore();
    private KeyValueStore authorizationCodeStore = new MemoryStore();
    private KeyValueStore accessTokenStore = new MemoryStore();
    private KeyValueStore oidcSessionStore = new MemoryStore();

    @Override
    protected ComponentLifecycle<StoreProvider> lifecycle() {
        return new ComponentLifecycle<>() {
            @Override
            public void start(StoreProvider provider) {
                // Stores are already set by the SystemFactory before start
            }

            @Override
            public void stop(StoreProvider provider) {
                // Do not null out stores — let GC handle cleanup.
                // This allows safe restart in the same JVM (test harnesses, hot-reload).
            }
        };
    }

    public KeyValueStore getStore(StoreType storeType) {
        return switch (storeType) {
            case BOUNCR_TOKEN -> bouncrTokenStore;
            case REFRESH_TOKEN -> refreshTokenStore;
            case AUTHORIZATION_CODE -> authorizationCodeStore;
            case ACCESS_TOKEN -> accessTokenStore;
            case OIDC_SESSION -> oidcSessionStore;
        };
    }

    public void setBouncrTokenStore(KeyValueStore store) { this.bouncrTokenStore = store; }
    public void setRefreshTokenStore(KeyValueStore store) { this.refreshTokenStore = store; }
    public void setAuthorizationCodeStore(KeyValueStore store) { this.authorizationCodeStore = store; }
    public void setAccessTokenStore(KeyValueStore store) { this.accessTokenStore = store; }
    public void setOidcSessionStore(KeyValueStore store) { this.oidcSessionStore = store; }

    public enum StoreType {
        BOUNCR_TOKEN,
        REFRESH_TOKEN,
        AUTHORIZATION_CODE,
        ACCESS_TOKEN,
        OIDC_SESSION
    }
}
