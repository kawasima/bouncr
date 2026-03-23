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
        String home_url,
        String callback_url,
        String description,
        String backchannel_logout_uri,
        String frontchannel_logout_uri,
        List<String> grant_types
) {
    public static OidcApplicationCreatedResponse of(OidcApplication app, String plaintextSecret) {
        return new OidcApplicationCreatedResponse(
                app.id(),
                app.name(),
                app.clientId(),
                plaintextSecret,
                app.homeUrl() != null ? app.homeUrl().toString() : null,
                app.callbackUrl() != null ? app.callbackUrl().toString() : null,
                app.description(),
                app.backchannelLogoutUri() != null ? app.backchannelLogoutUri().toString() : null,
                app.frontchannelLogoutUri() != null ? app.frontchannelLogoutUri().toString() : null,
                app.grantTypes() != null
                        ? app.grantTypes().stream().map(GrantType::getValue).toList()
                        : GrantType.DEFAULT_GRANT_TYPES);
    }
}
