package net.unit8.bouncr.api.boundary;

/**
 * Request body for authenticating with a WebAuthn credential.
 *
 * @param authenticationResponseJSON JSON-serialized WebAuthn authentication response from the authenticator
 */
public record WebAuthnAuthenticate(String authenticationResponseJSON) {}
