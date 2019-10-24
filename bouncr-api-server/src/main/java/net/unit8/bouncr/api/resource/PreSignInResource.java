package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.data.Cookie;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OidcSession;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
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
        OidcSession oidcSession = OidcSession.create(config.getSecureRandom());
        storeProvider.getStore(OIDC_SESSION).write(oidcSessionId, oidcSession);

        Cookie cookie = Cookie.create(COOKIE_NAME, oidcSessionId);
        ZoneId zone = ZoneId.systemDefault();
        Date expires = Date.from(
                ZonedDateTime.of(LocalDateTime.now()
                        .plusSeconds(config.getOidcSessionExpires()), zone)
                        .toInstant());
        cookie.setExpires(expires);
        cookie.setPath("/");
        context.setHeaders(Headers.of("Set-Cookie", cookie.toHttpString()));

        return oidcSession;
    }
}
