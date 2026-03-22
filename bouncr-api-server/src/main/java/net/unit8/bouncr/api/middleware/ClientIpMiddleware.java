package net.unit8.bouncr.api.middleware;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.WebMiddleware;

/**
 * Resolves the real client IP from the X-Forwarded-For header set by Envoy
 * and overwrites {@code remoteAddr} so downstream middleware and resources
 * see the true client address instead of the proxy's.
 */
@Middleware(name = "clientIp")
public class ClientIpMiddleware implements WebMiddleware {
    @Override
    public <NNREQ, NNRES> HttpResponse handle(HttpRequest request,
            MiddlewareChain<HttpRequest, HttpResponse, NNREQ, NNRES> chain) {
        String xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Leftmost entry is the original client IP
            String clientIp = xff.split(",")[0].trim();
            request.setRemoteAddr(clientIp);
        }
        return castToHttpResponse(chain.next(request));
    }
}
