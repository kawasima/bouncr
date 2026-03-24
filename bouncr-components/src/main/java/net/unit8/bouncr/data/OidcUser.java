package net.unit8.bouncr.data;

/**
 * Link between a local {@link User} and an account in an external OIDC provider.
 *
 * @param providerName display name of the OIDC provider
 * @param user         local user account
 * @param oidcSub      subject identifier from the provider
 */
public record OidcUser(
    String providerName,
    User user,
    String oidcSub
) {}
