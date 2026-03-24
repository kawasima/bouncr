package net.unit8.bouncr.api.boundary;

import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;

import java.util.List;

/**
 * Response returned once after creating an OIDC application.
 * Includes the plaintext client_secret which is never stored or retrievable again.
 */
public record OidcApplicationCreatedResponse(
        Long id,
        String name,
        String client_id,
        String client_secret,
        String home_uri,
        String callback_uri,
        String description,
        String backchannel_logout_uri,
        String frontchannel_logout_uri,
        List<String> grant_types
) {
    public static OidcApplicationCreatedResponse of(OidcApplication app, String plaintextSecret) {
        var meta = app.metadata();
        var grantTypes = meta != null && meta.grantTypes() != null
                ? meta.grantTypes().stream().map(GrantType::getValue).toList()
                : GrantType.DEFAULT_GRANT_TYPES;
        return new OidcApplicationCreatedResponse(
                app.id(),
                app.name(),
                app.credentials().clientId(),
                plaintextSecret,
                meta != null && meta.homeUri() != null ? meta.homeUri().toString() : null,
                meta != null && meta.callbackUri() != null ? meta.callbackUri().toString() : null,
                app.description(),
                meta != null && meta.backchannelLogoutUri() != null ? meta.backchannelLogoutUri().toString() : null,
                meta != null && meta.frontchannelLogoutUri() != null ? meta.frontchannelLogoutUri().toString() : null,
                grantTypes);
    }
}
