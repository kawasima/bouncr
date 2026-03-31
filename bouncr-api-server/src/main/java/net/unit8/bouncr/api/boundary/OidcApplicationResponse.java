package net.unit8.bouncr.api.boundary;

import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.OidcClientMetadata;

import java.util.List;
import java.util.Map;

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
        List<Map<String, Object>> permissions,
        List<String> grant_types
) {
    public static OidcApplicationResponse of(OidcApplication app) {
        OidcClientMetadata meta = app.metadata();
        var grantTypes = meta != null && meta.grantTypes() != null
                ? meta.grantTypes().stream().map(GrantType::getValue).toList()
                : GrantType.DEFAULT_GRANT_TYPES;
        var permissions = app.permissions() != null
                ? app.permissions().stream().map(BouncrJsonEncoders.PERMISSION::encode).toList()
                : List.<Map<String, Object>>of();
        return new OidcApplicationResponse(
                app.id(),
                app.name(),
                app.credentials().clientId(),
                meta != null ? meta.homeUriString() : null,
                meta != null ? meta.callbackUriString() : null,
                app.description(),
                meta != null ? meta.backchannelLogoutUriString() : null,
                meta != null ? meta.frontchannelLogoutUriString() : null,
                permissions,
                grantTypes);
    }
}
