package net.unit8.bouncr.api.middleware;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.web.data.HttpRequest;
import enkan.web.data.HttpResponse;
import enkan.web.middleware.WebMiddleware;

/**
 * Resolves the real client IP from the X-Forwarded-For header set by Envoy
 * and overwrites {@code remoteAddr} so downstream middleware and resources
 * see the true client address instead of the proxy's.
 *
 * <p><strong>Trust assumption:</strong> this middleware trusts the leftmost entry of
 * {@code X-Forwarded-For} unconditionally. It is only safe when Envoy (configured with
 * {@code use_remote_address: true}) is the sole entry point — direct access to the
 * api-server would allow a client to spoof its IP.
 */
@Middleware(name = "clientIp")
public class ClientIpMiddleware implements WebMiddleware {
    @Override
    public <NNREQ, NNRES> HttpResponse handle(HttpRequest request,
            MiddlewareChain<HttpRequest, HttpResponse, NNREQ, NNRES> chain) {
        String xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Leftmost entry is the original client IP
            int comma = xff.indexOf(',');
            String clientIp = (comma >= 0 ? xff.substring(0, comma) : xff).trim();
            request.setRemoteAddr(clientIp);
        }
        return castToHttpResponse(chain.next(request));
    }
}
