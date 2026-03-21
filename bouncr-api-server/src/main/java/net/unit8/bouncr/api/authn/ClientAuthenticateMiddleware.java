package net.unit8.bouncr.api.authn;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.data.PrincipalAvailable;
import enkan.middleware.WebMiddleware;
import enkan.util.MixinUtils;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * The logging for client authentication.
 *
 * A HTTP header "X-Client-DN" was set by Undertow client authentication.
 *
 * @author kawasima
 */
@Middleware(name = "ClientAuthenticate", dependencies = {"authenticate"})
public class ClientAuthenticateMiddleware implements WebMiddleware {
    private boolean isAuthenticated(PrincipalAvailable request) {
        return request.getPrincipal() != null;
    }

    @Override
    public <NNREQ, NNRES> HttpResponse handle(HttpRequest request, MiddlewareChain<HttpRequest, HttpResponse, NNREQ, NNRES> chain) {
        request = MixinUtils.mixin(request, PrincipalAvailable.class);
        String clientDN = request.getHeaders().get("X-Client-DN");
        if (!isAuthenticated(request) && clientDN != null) {
            String account = extractCommonName(clientDN);

        }
        return chain.next(request);
    }

    static String extractCommonName(String dn) {
        try {
            LdapName ldapName = new LdapName(dn);
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
            return null;
        } catch (InvalidNameException e) {
            return null;
        }
    }
}
