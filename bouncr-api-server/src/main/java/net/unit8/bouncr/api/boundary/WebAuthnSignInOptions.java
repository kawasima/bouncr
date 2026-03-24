package net.unit8.bouncr.api.boundary;

/**
 * Request body for fetching WebAuthn authentication options.
 *
 * @param account user account name to generate authentication options for
 */
public record WebAuthnSignInOptions(String account) {}
