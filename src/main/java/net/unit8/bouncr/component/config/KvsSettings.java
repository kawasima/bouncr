package net.unit8.bouncr.component.config;

import enkan.component.SystemComponent;
import enkan.middleware.session.JCacheStore;
import enkan.middleware.session.KeyValueStore;

import javax.cache.configuration.Factory;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class KvsSettings implements Serializable {
    private Function<Map<String, SystemComponent>, KeyValueStore> bouncrTokenStoreFactory =
            deps -> {
                Factory<ExpiryPolicy> expiryPolicyFactory = AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3600));
                return new JCacheStore(expiryPolicyFactory);
            };
    private Function<Map<String, SystemComponent>, KeyValueStore> authorizationCodeStoreFactory =
            deps -> {
                Factory<ExpiryPolicy> expiryPolicyFactory = AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 180));
                return new JCacheStore(expiryPolicyFactory);
            };

    private Function<Map<String, SystemComponent>, KeyValueStore> accessTokenStoreFactory =
            deps -> {
                Factory<ExpiryPolicy> expiryPolicyFactory = AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3600));
                return new JCacheStore(expiryPolicyFactory);
            };

    private Function<Map<String, SystemComponent>, KeyValueStore> oidcSessionStoreFactory =
            deps -> {
                Factory<ExpiryPolicy> expiryPolicyFactory = AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3600));
                return new JCacheStore(expiryPolicyFactory);
            };

    public Function<Map<String, SystemComponent>, KeyValueStore> getBouncrTokenStoreFactory() {
        return bouncrTokenStoreFactory;
    }

    public void setBouncrTokenStoreFactory(Function<Map<String, SystemComponent>, KeyValueStore> bouncrTokenStoreFactory) {
        this.bouncrTokenStoreFactory = bouncrTokenStoreFactory;
    }

    public Function<Map<String, SystemComponent>, KeyValueStore> getAuthorizationCodeStoreFactory() {
        return authorizationCodeStoreFactory;
    }

    public void setAuthorizationCodeStoreFactory(Function<Map<String, SystemComponent>, KeyValueStore> authorizationCodeStoreFactory) {
        this.authorizationCodeStoreFactory = authorizationCodeStoreFactory;
    }

    public Function<Map<String, SystemComponent>, KeyValueStore> getAccessTokenStoreFactory() {
        return accessTokenStoreFactory;
    }

    public void setAccessTokenStoreFactory(Function<Map<String, SystemComponent>, KeyValueStore> accessTokenStoreFactory) {
        this.accessTokenStoreFactory = accessTokenStoreFactory;
    }

    public Function<Map<String, SystemComponent>, KeyValueStore> getOidcSessionStoreFactory() {
        return oidcSessionStoreFactory;
    }

    public void setOidcSessionStoreFactory(Function<Map<String, SystemComponent>, KeyValueStore> oidcSessionStoreFactory) {
        this.oidcSessionStoreFactory = oidcSessionStoreFactory;
    }
}
