package net.unit8.bouncr.api.authn;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.data.PrincipalAvailable;
import enkan.middleware.AbstractWebMiddleware;
import enkan.util.MixinUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

/**
 * The logging for client authentication.
 *
 * A HTTP header "X-Client-DN" was set by Undertow client authentication.
 *
 * @author kawasima
 */
@Middleware(name = "ClientAuthenticate", dependencies = {"authenticate"})
public class ClientAuthenticateMiddleware<NRES> extends AbstractWebMiddleware<HttpRequest, NRES> {
    private boolean isAuthenticated(PrincipalAvailable request) {
        return request.getPrincipal() != null;
    }

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain<HttpRequest, NRES, ?, ?> chain) {
        request = MixinUtils.mixin(request, PrincipalAvailable.class);
        String clientDN = request.getHeaders().get("X-Client-DN");
        if (!isAuthenticated(request) && clientDN != null) {
            RDN cn = new X500Name(clientDN).getRDNs(BCStyle.CN)[0];
            String account = IETFUtils.valueToString(cn.getFirst().getValue());

        }
        return castToHttpResponse(chain.next(request));
    }
}
