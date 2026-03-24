package net.unit8.bouncr.api.boundary;

import java.util.List;

/**
 * Request body for registering an OIDC client application.
 *
 * @param name display name of the OIDC application
 * @param grantTypes OAuth 2.0 grant types the application may use
 * @param homeUri home page URI of the application
 * @param callbackUri OAuth callback URI for authorization code flow
 * @param description human-readable description of the application
 * @param backchannelLogoutUri URI for backchannel logout notifications
 * @param frontchannelLogoutUri URI for frontchannel logout redirects
 * @param permissions list of permission names granted to the application
 */
public record OidcApplicationCreate(String name, List<String> grantTypes, String homeUri, String callbackUri,
                                     String description, String backchannelLogoutUri, String frontchannelLogoutUri,
                                     List<String> permissions) {}
