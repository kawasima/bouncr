package net.unit8.bouncr.authn;

import enkan.component.BeansConverter;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.security.AuthBackend;
import enkan.util.ThreadingUtils;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.authz.UserPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.entity.Realm;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Objects;

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
                UserPrincipal user = beans.createFrom(storeProvider.getStore(BOUNCR_TOKEN).read(token), UserPrincipal.class);
                if (user != null) {
                    return new UserPermissionPrincipal(user.getId(), user.getName(), user.getProfiles(), user.getPermissions(realm.getId()));
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
