package net.unit8.bouncr.api.boundary;

import java.util.List;
import net.unit8.raoh.Presence;

/**
 * Request body for updating an OIDC client application.
 * Uses {@link net.unit8.raoh.Presence} for tri-state PATCH semantics.
 *
 * @param name display name of the OIDC application
 * @param grantTypes OAuth 2.0 grant types the application may use
 * @param homeUri home page URI (absent means no change, present-null means clear)
 * @param callbackUri OAuth callback URI (absent means no change, present-null means clear)
 * @param description human-readable description (absent means no change, present-null means clear)
 * @param backchannelLogoutUri URI for backchannel logout notifications (tri-state)
 * @param frontchannelLogoutUri URI for frontchannel logout redirects (tri-state)
 * @param permissions list of permission names granted to the application
 */
public record OidcApplicationUpdate(String name, List<String> grantTypes, Presence<String> homeUri,
                                     Presence<String> callbackUri, Presence<String> description,
                                     Presence<String> backchannelLogoutUri, Presence<String> frontchannelLogoutUri,
                                     List<String> permissions) {}
