package net.unit8.bouncr.proxy.authn;

import enkan.component.BeansConverter;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.security.AuthBackend;
import enkan.security.bouncr.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.entity.Realm;

import javax.inject.Inject;
import java.security.Principal;

import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;

public class BouncrStoreBackend implements AuthBackend<HttpRequest, UserPermissionPrincipal> {
    @Inject
    private StoreProvider storeProvider;

    @Inject
    private RealmCache realmCache;

    @Inject
    private BeansConverter beans;

    @Inject
    private BouncrConfiguration config;

    private String parseTokenFromCookie(HttpRequest request) {
        Cookie tokenCookie = request.getCookies().get(config.getTokenName());
        return tokenCookie != null ? tokenCookie.getValue() : null;
    }

    private String parseTokenFromHeader(HttpRequest request) {
        return request.getHeaders().get("X-Bouncr-Token");
    }

    @Override
    public UserPermissionPrincipal parse(HttpRequest request) {
        String token = parseTokenFromCookie(request);
        if (token == null) {
            token = parseTokenFromHeader(request);
        }

        if (token != null) {
            Realm realm = realmCache.matches(request.getUri());
            if (realm != null) {
                UserPermissionPrincipal user = beans.createFrom(storeProvider.getStore(BOUNCR_TOKEN).read(token), UserPermissionPrincipal.class);
                if (user != null) {
                    return new UserPermissionPrincipal(user.getName(), user.getProfiles(), user.getPermissions());
                }
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
