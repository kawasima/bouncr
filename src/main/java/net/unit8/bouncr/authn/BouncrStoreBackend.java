package net.unit8.bouncr.authn;

import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.security.AuthBackend;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.entity.Realm;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Map;

import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;

public class BouncrStoreBackend implements AuthBackend<HttpRequest, UserPermissionPrincipal> {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private RealmCache realmCache;

    @Inject
    private BouncrConfiguration config;

    @Override
    public UserPermissionPrincipal parse(HttpRequest httpRequest) {
        Cookie tokenCookie = httpRequest.getCookies().get(config.getTokenName());
        Realm realm = realmCache.matches(httpRequest.getUri());
        if (tokenCookie != null && realm != null) {
            Map<Long, UserPermissionPrincipal> perms = (Map<Long, UserPermissionPrincipal>) storeProvider.getStore(BOUNCR_TOKEN).read(tokenCookie.getValue());
            if (perms != null) {
                return perms.get(realm.getId());
            }
        }
        return null;
    }

    @Override
    public Principal authenticate(HttpRequest httpRequest, UserPermissionPrincipal userPermissionPrincipal) {
        return userPermissionPrincipal;
    }

    public void setStoreProvider(StoreProvider storeProvider) {
        this.storeProvider = storeProvider;
    }

    public void setRealmCache(RealmCache realmCache) {
        this.realmCache = realmCache;
    }
}
