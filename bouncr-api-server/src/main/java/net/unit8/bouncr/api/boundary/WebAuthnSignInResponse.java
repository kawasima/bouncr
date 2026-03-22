package net.unit8.bouncr.api.boundary;

/**
 * Response body for WebAuthn passkey sign-in.
 */
public record WebAuthnSignInResponse(
        String token,
        String account
) {}
