package net.unit8.bouncr.api.util;

import enkan.web.data.Cookie;
import enkan.web.data.HostCookie;
import net.unit8.bouncr.component.BouncrConfiguration;

/**
 * Cookie factory for consistent cookie construction across all resources.
 * Uses enkan's Cookie API with SameSite and HostCookie support (enkan 0.15.0+).
 */
public final class BouncrCookies {
    private final BouncrConfiguration config;

    public BouncrCookies(BouncrConfiguration config) {
        this.config = config;
    }

    /** Creates a token cookie (SameSite=Strict) for sign-in responses. */
    public Cookie token(String value) {
        HostCookie c = HostCookie.create(config.getTokenName(), value);
        c.setHttpOnly(true);
        c.setSameSite("Strict");
        c.setMaxAge((int) config.getRefreshTokenExpires());
        return c;
    }

    /** Creates a cookie that clears the token (Max-Age=0). */
    public Cookie clearToken() {
        HostCookie c = HostCookie.create(config.getTokenName(), "");
        c.setHttpOnly(true);
        c.setSameSite("Strict");
        c.setMaxAge(0);
        return c;
    }

    /** Creates a session cookie (SameSite=Lax) for OIDC/WebAuthn flows. */
    public Cookie session(String name, String value, int maxAge) {
        Cookie c = Cookie.create(name, value);
        c.setHttpOnly(true);
        c.setSecure(config.isSecureCookie());
        c.setSameSite("Lax");
        c.setMaxAge(maxAge);
        c.setPath("/");
        return c;
    }

    /** Creates a cookie that clears a session (Max-Age=0). */
    public Cookie clearSession(String name) {
        return session(name, "", 0);
    }
}
