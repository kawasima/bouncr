package net.unit8.bouncr.component.config;

import enkan.component.SystemComponent;
import enkan.middleware.session.KeyValueStore;
import enkan.middleware.session.MemoryStore;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

public class KvsSettings implements Serializable {
    private Function<Map<String, SystemComponent<?>>, KeyValueStore> bouncrTokenStoreFactory =
            deps -> new MemoryStore();
    private Function<Map<String, SystemComponent<?>>, KeyValueStore> authorizationCodeStoreFactory =
            deps -> new MemoryStore();
    private Function<Map<String, SystemComponent<?>>, KeyValueStore> accessTokenStoreFactory =
            deps -> new MemoryStore();
    private Function<Map<String, SystemComponent<?>>, KeyValueStore> oidcSessionStoreFactory =
            deps -> new MemoryStore();

    public Function<Map<String, SystemComponent<?>>, KeyValueStore> getBouncrTokenStoreFactory() {
        return bouncrTokenStoreFactory;
    }

    public void setBouncrTokenStoreFactory(Function<Map<String, SystemComponent<?>>, KeyValueStore> bouncrTokenStoreFactory) {
        this.bouncrTokenStoreFactory = bouncrTokenStoreFactory;
    }

    public Function<Map<String, SystemComponent<?>>, KeyValueStore> getAuthorizationCodeStoreFactory() {
        return authorizationCodeStoreFactory;
    }

    public void setAuthorizationCodeStoreFactory(Function<Map<String, SystemComponent<?>>, KeyValueStore> authorizationCodeStoreFactory) {
        this.authorizationCodeStoreFactory = authorizationCodeStoreFactory;
    }

    public Function<Map<String, SystemComponent<?>>, KeyValueStore> getAccessTokenStoreFactory() {
        return accessTokenStoreFactory;
    }

    public void setAccessTokenStoreFactory(Function<Map<String, SystemComponent<?>>, KeyValueStore> accessTokenStoreFactory) {
        this.accessTokenStoreFactory = accessTokenStoreFactory;
    }

    public Function<Map<String, SystemComponent<?>>, KeyValueStore> getOidcSessionStoreFactory() {
        return oidcSessionStoreFactory;
    }

    public void setOidcSessionStoreFactory(Function<Map<String, SystemComponent<?>>, KeyValueStore> oidcSessionStoreFactory) {
        this.oidcSessionStoreFactory = oidcSessionStoreFactory;
    }
}
