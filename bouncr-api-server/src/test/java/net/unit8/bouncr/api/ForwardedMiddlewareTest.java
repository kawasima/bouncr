package net.unit8.bouncr.api;

import enkan.Middleware;
import enkan.MiddlewareChain;
import enkan.web.collection.Headers;
import enkan.web.data.DefaultHttpRequest;
import enkan.web.data.HttpRequest;
import enkan.web.data.HttpResponse;
import enkan.web.middleware.ForwardedMiddleware;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for ForwardedMiddleware as configured in BouncrApplicationFactory:
 * trusted proxy CIDR = 127.0.0.0/8, preferStandard = true (default).
 */
class ForwardedMiddlewareTest {
    private final ForwardedMiddleware middleware = builder(new ForwardedMiddleware())
            .set(ForwardedMiddleware::setTrustedProxies,
                    List.of("127.0.0.0/8"))
            .build();

    private static MiddlewareChain<HttpRequest, HttpResponse, HttpRequest, HttpResponse> noopChain() {
        return new MiddlewareChain<>() {
            @Override public MiddlewareChain<HttpRequest, HttpResponse, HttpRequest, HttpResponse> setNext(MiddlewareChain<HttpRequest, HttpResponse, ?, ?> next) { return this; }
            @Override public Middleware<HttpRequest, HttpResponse, HttpRequest, HttpResponse> getMiddleware() { return null; }
            @Override public String getName() { return "noop"; }
            @Override public Predicate<? super HttpRequest> getPredicate() { return null; }
            @Override public void setPredicate(Predicate<? super HttpRequest> predicate) {}
            @Override public HttpResponse next(HttpRequest req) { return null; }
        };
    }

    private String resolvedAddr(String remoteAddr, String xff) {
        DefaultHttpRequest req = new DefaultHttpRequest();
        req.setRemoteAddr(remoteAddr);
        req.setHeaders(Headers.empty());
        if (xff != null) {
            req.getHeaders().put("X-Forwarded-For", xff);
        }
        middleware.handle(req, noopChain());
        return req.getRemoteAddr();
    }

    private String resolvedAddrViaForwarded(String remoteAddr, String forwarded) {
        DefaultHttpRequest req = new DefaultHttpRequest();
        req.setRemoteAddr(remoteAddr);
        req.setHeaders(Headers.empty());
        req.getHeaders().put("Forwarded", forwarded);
        middleware.handle(req, noopChain());
        return req.getRemoteAddr();
    }

    // --- X-Forwarded-For (legacy) tests ---

    @Test
    void trustedProxyXffHeaderIsApplied() {
        // Request arrives from loopback (trusted proxy) — XFF header should be trusted
        assertThat(resolvedAddr("127.0.0.1", "203.0.113.1")).isEqualTo("203.0.113.1");
    }

    @Test
    void leftmostXffEntryIsUsedForMultiHop() {
        // "client, proxy1, proxy2" — leftmost is the original client
        assertThat(resolvedAddr("127.0.0.1", "203.0.113.1, 10.0.0.1, 10.0.0.2"))
                .isEqualTo("203.0.113.1");
    }

    @Test
    void untrustedProxyXffHeaderIsIgnored() {
        // Request arrives from an untrusted IP — XFF header must not be applied
        assertThat(resolvedAddr("203.0.113.99", "1.2.3.4")).isEqualTo("203.0.113.99");
    }

    @Test
    void missingHeaderLeavesRemoteAddrUnchanged() {
        assertThat(resolvedAddr("127.0.0.1", null)).isEqualTo("127.0.0.1");
    }

    // --- RFC 7239 Forwarded header tests (preferStandard=true by default) ---

    @Test
    void trustedProxyRfc7239ForwardedHeaderIsApplied() {
        // Envoy may inject the standard Forwarded: header (RFC 7239) instead of XFF
        assertThat(resolvedAddrViaForwarded("127.0.0.1", "for=203.0.113.1"))
                .isEqualTo("203.0.113.1");
    }

    @Test
    void rfc7239ForwardedTakesPrecedenceOverXff() {
        // preferStandard=true: Forwarded header wins when both are present
        DefaultHttpRequest req = new DefaultHttpRequest();
        req.setRemoteAddr("127.0.0.1");
        req.setHeaders(Headers.empty());
        req.getHeaders().put("Forwarded", "for=203.0.113.1");
        req.getHeaders().put("X-Forwarded-For", "1.2.3.4");
        middleware.handle(req, noopChain());
        assertThat(req.getRemoteAddr()).isEqualTo("203.0.113.1");
    }

    @Test
    void rfc7239ForwardedUntrustedProxyIsIgnored() {
        assertThat(resolvedAddrViaForwarded("203.0.113.99", "for=1.2.3.4"))
                .isEqualTo("203.0.113.99");
    }
}
