package net.unit8.bouncr.api.boundary;

/**
 * Request body for registering a WebAuthn credential.
 *
 * @param registrationResponseJSON JSON-serialized WebAuthn registration response from the authenticator
 * @param credentialName user-assigned display name for the credential
 */
public record WebAuthnRegister(String registrationResponseJSON, String credentialName) {}
