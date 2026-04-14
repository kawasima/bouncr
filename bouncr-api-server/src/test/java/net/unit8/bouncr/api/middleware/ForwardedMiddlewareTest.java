package net.unit8.bouncr.api.middleware;

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

    @Test
    void trustedProxyHeaderIsApplied() {
        // Request arrives from loopback (trusted proxy) — XFF header should be trusted
        assertThat(resolvedAddr("127.0.0.1", "203.0.113.1")).isEqualTo("203.0.113.1");
    }

    @Test
    void leftmostEntryIsUsedForMultiHop() {
        // "client, proxy1, proxy2" — leftmost is the original client
        assertThat(resolvedAddr("127.0.0.1", "203.0.113.1, 10.0.0.1, 10.0.0.2"))
                .isEqualTo("203.0.113.1");
    }

    @Test
    void untrustedProxyHeaderIsIgnored() {
        // Request arrives from an untrusted IP — XFF header must not be applied
        assertThat(resolvedAddr("203.0.113.99", "1.2.3.4")).isEqualTo("203.0.113.99");
    }

    @Test
    void missingHeaderLeavesRemoteAddrUnchanged() {
        assertThat(resolvedAddr("127.0.0.1", null)).isEqualTo("127.0.0.1");
    }
}
