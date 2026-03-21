package net.unit8.bouncr.data;

/**
 * Link between a local {@link User} and an account in an {@link OidcProvider}.
 *
 * @param oidcProvider external provider
 * @param user local user account
 * @param oidcSub subject identifier from the provider
 */
public record OidcUser(
    OidcProvider oidcProvider,
    User user,
    String oidcSub
) {}
