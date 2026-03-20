package net.unit8.bouncr.data;

public record OidcInvitation(
        Long id,
        Invitation invitation,
        OidcProvider oidcProvider,
        String oidcPayload
) {}
