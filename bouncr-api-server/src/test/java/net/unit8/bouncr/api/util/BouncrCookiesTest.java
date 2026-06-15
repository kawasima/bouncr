package net.unit8.bouncr.api.util;

import enkan.web.data.Cookie;
import enkan.web.data.HostCookie;
import net.unit8.bouncr.component.BouncrConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BouncrCookiesTest {
    private BouncrCookies cookies;

    @BeforeEach
    void setUp() {
        BouncrConfiguration config = new BouncrConfiguration();
        cookies = new BouncrCookies(config);
    }

    @Test
    void tokenIsHostCookie() {
        Cookie c = cookies.token("abc123");
        assertThat(c).isInstanceOf(HostCookie.class);
    }

    @Test
    void tokenHasHostPrefix() {
        Cookie c = cookies.token("abc123");
        assertThat(c.toHttpString()).startsWith("__Host-");
    }

    @Test
    void tokenIsHttpOnly() {
        Cookie c = cookies.token("abc123");
        assertThat(c.isHttpOnly()).isTrue();
    }

    @Test
    void tokenIsSecure() {
        Cookie c = cookies.token("abc123");
        assertThat(c.isSecure()).isTrue();
    }

    @Test
    void tokenHasSameSiteStrict() {
        Cookie c = cookies.token("abc123");
        assertThat(c.toHttpString()).containsIgnoringCase("samesite=strict");
    }

    @Test
    void tokenHasPositiveMaxAge() {
        Cookie c = cookies.token("abc123");
        assertThat(c.getMaxAge()).isPositive();
    }

    @Test
    void clearTokenIsHostCookie() {
        Cookie c = cookies.clearToken();
        assertThat(c).isInstanceOf(HostCookie.class);
    }

    @Test
    void clearTokenHasZeroMaxAge() {
        Cookie c = cookies.clearToken();
        assertThat(c.getMaxAge()).isZero();
    }

    @Test
    void clearTokenHasEmptyValue() {
        Cookie c = cookies.clearToken();
        assertThat(c.getValue()).isEmpty();
    }

    @Test
    void sessionCookieIsNotHostCookie() {
        Cookie c = cookies.session("OIDC_SESSION", "xyz", 300);
        assertThat(c).isNotInstanceOf(HostCookie.class);
    }

    @Test
    void sessionCookieHasSameSiteLax() {
        Cookie c = cookies.session("OIDC_SESSION", "xyz", 300);
        assertThat(c.toHttpString()).containsIgnoringCase("samesite=lax");
    }

    @Test
    void sessionCookieHasExpectedMaxAge() {
        Cookie c = cookies.session("OIDC_SESSION", "xyz", 300);
        assertThat(c.getMaxAge()).isEqualTo(300);
    }
}
