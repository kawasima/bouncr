package net.unit8.bouncr.data;

/**
 * Composite key for {@link OidcUser}.
 *
 * @param oidcProvider OIDC provider ID
 * @param user user ID
 */
public record OidcUserId(Long oidcProvider, Long user) {}
