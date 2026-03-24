package net.unit8.bouncr.api.boundary;

import net.unit8.bouncr.data.GrantType;
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
        String home_uri,
        String callback_uri,
        String description,
        String backchannel_logout_uri,
        String frontchannel_logout_uri,
        List<Permission> permissions,
        List<String> grant_types
) {
    public static OidcApplicationResponse of(OidcApplication app) {
        var meta = app.metadata();
        var grantTypes = meta != null && meta.grantTypes() != null
                ? meta.grantTypes().stream().map(GrantType::getValue).toList()
                : GrantType.DEFAULT_GRANT_TYPES;
        return new OidcApplicationResponse(
                app.id(),
                app.name(),
                app.credentials().clientId(),
                meta != null && meta.homeUri() != null ? meta.homeUri().toString() : null,
                meta != null && meta.callbackUri() != null ? meta.callbackUri().toString() : null,
                app.description(),
                meta != null && meta.backchannelLogoutUri() != null ? meta.backchannelLogoutUri().toString() : null,
                meta != null && meta.frontchannelLogoutUri() != null ? meta.frontchannelLogoutUri().toString() : null,
                app.permissions() != null ? app.permissions() : List.of(),
                grantTypes);
    }
}
