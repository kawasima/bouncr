package net.unit8.bouncr.api.authn;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.WebMiddleware;

/**
 * The logging for client authentication.
 *
 * A HTTP header "X-Client-DN" was set by Undertow client authentication.
 *
 * @author kawasima
 */
@Middleware(name = "ClientAuthenticate", dependencies = {"authenticate"})
public class ClientAuthenticateMiddleware implements WebMiddleware {
    @Override
    public <NNREQ, NNRES> HttpResponse handle(HttpRequest request, MiddlewareChain<HttpRequest, HttpResponse, NNREQ, NNRES> chain) {
        // X-Client-DN handling is intentionally disabled until principal mapping
        // from DN to local account is implemented.
        return chain.next(request);
    }
}
