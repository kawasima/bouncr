package net.unit8.bouncr.data;

/**
 * Invitation entry that links a user invitation to an external OIDC provider.
 *
 * @param id persistent identifier
 * @param invitation parent invitation
 * @param oidcProvider target OIDC provider
 * @param oidcPayload provider-specific payload captured during invitation
 */
public record OidcInvitation(
        Long id,
        Invitation invitation,
        OidcProvider oidcProvider,
        String oidcPayload
) {}
