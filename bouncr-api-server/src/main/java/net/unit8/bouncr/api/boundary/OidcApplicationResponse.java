package net.unit8.bouncr.api.boundary;

import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.Permission;

import java.util.List;

/**
 * Public view of an OIDC application, excluding sensitive fields
 * (client_secret hash, private_key).
 */
public record OidcApplicationResponse(
        Long id,
        String name,
        String client_id,
        String home_url,
        String callback_url,
        String description,
        String backchannel_logout_uri,
        String frontchannel_logout_uri,
        List<Permission> permissions
) {
    public static OidcApplicationResponse of(OidcApplication app) {
        return new OidcApplicationResponse(
                app.id(),
                app.name(),
                app.clientId(),
                app.homeUrl() != null ? app.homeUrl().toString() : null,
                app.callbackUrl() != null ? app.callbackUrl().toString() : null,
                app.description(),
                app.backchannelLogoutUri() != null ? app.backchannelLogoutUri().toString() : null,
                app.frontchannelLogoutUri() != null ? app.frontchannelLogoutUri().toString() : null,
                app.permissions());
    }
}
