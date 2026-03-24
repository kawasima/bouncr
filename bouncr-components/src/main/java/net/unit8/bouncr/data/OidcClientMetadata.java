package net.unit8.bouncr.data;

import java.net.URI;
import java.util.Set;

/**
 * OIDC client registration metadata (RFC 7591).
 *
 * @param homeUri               home page URI (Bouncr extension)
 * @param callbackUri           redirect URI for authorization code flow (RFC 6749)
 * @param backchannelLogoutUri  back-channel logout endpoint (OpenID Back-Channel Logout)
 * @param frontchannelLogoutUri front-channel logout endpoint (OpenID Front-Channel Logout)
 * @param grantTypes            allowed OAuth2 grant types
 */
public record OidcClientMetadata(
    URI homeUri,
    URI callbackUri,
    URI backchannelLogoutUri,
    URI frontchannelLogoutUri,
    Set<GrantType> grantTypes
) {}
