package net.unit8.bouncr.api.middleware;

import enkan.Middleware;
import enkan.MiddlewareChain;
import enkan.web.collection.Headers;
import enkan.web.data.DefaultHttpRequest;
import enkan.web.data.HttpRequest;
import enkan.web.data.HttpResponse;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpMiddlewareTest {
    private final ClientIpMiddleware middleware = new ClientIpMiddleware();

    /** Stub chain that records the request passed to next() and returns null. */
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

    private String resolvedAddr(String xff) {
        DefaultHttpRequest req = new DefaultHttpRequest();
        req.setRemoteAddr("127.0.0.1");
        req.setHeaders(Headers.empty());
        if (xff != null) {
            req.getHeaders().put("X-Forwarded-For", xff);
        }
        middleware.handle(req, noopChain());
        return req.getRemoteAddr();
    }

    @Test
    void singleEntryIsUsedDirectly() {
        assertThat(resolvedAddr("203.0.113.1")).isEqualTo("203.0.113.1");
    }

    @Test
    void leftmostEntryIsUsedInMultiHop() {
        // "client, proxy1, proxy2" — leftmost is the original client
        assertThat(resolvedAddr("203.0.113.1, 10.0.0.1, 10.0.0.2")).isEqualTo("203.0.113.1");
    }

    @Test
    void leadingAndTrailingSpacesAreTrimmed() {
        assertThat(resolvedAddr("  203.0.113.1  , 10.0.0.1")).isEqualTo("203.0.113.1");
    }

    @Test
    void missingHeaderLeavesRemoteAddrUnchanged() {
        assertThat(resolvedAddr(null)).isEqualTo("127.0.0.1");
    }

    @Test
    void blankHeaderLeavesRemoteAddrUnchanged() {
        assertThat(resolvedAddr("   ")).isEqualTo("127.0.0.1");
    }
}
