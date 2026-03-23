package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.util.BouncrCookies;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OidcSession;
import net.unit8.bouncr.util.RandomUtils;

import jakarta.inject.Inject;
import java.util.UUID;

import static kotowari.restful.DecisionPoint.POST;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;

@AllowedMethods({"POST"})
public class PreSignInResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private StoreProvider storeProvider;

    private static final String COOKIE_NAME = "OIDC_SESSION_ID";

    @Decision(POST)
    public OidcSession post(RestContext context) {
        String oidcSessionId = UUID.randomUUID().toString();
        String nonce = RandomUtils.generateRandomString(32, config.getSecureRandom());
        String state = RandomUtils.generateRandomString(8, config.getSecureRandom());
        OidcSession oidcSession = new OidcSession(nonce, state, null, null);
        storeProvider.getStore(OIDC_SESSION).write(oidcSessionId, oidcSession);

        BouncrCookies cookies = new BouncrCookies(config);
        String cookieHeader = cookies.session(COOKIE_NAME, oidcSessionId, (int) config.getOidcSessionExpires()).toHttpString();
        context.setHeaders(Headers.of("Set-Cookie", cookieHeader));

        return oidcSession;
    }
}
