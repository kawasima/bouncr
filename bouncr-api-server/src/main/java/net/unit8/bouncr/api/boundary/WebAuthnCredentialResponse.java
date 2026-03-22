package net.unit8.bouncr.api.boundary;

import net.unit8.bouncr.data.WebAuthnCredential;

/**
 * Public view of a WebAuthn credential for listing and registration responses.
 */
public record WebAuthnCredentialResponse(
        Long id,
        String credential_name,
        String transports,
        boolean discoverable
) {
    public static WebAuthnCredentialResponse of(WebAuthnCredential c) {
        return new WebAuthnCredentialResponse(
                c.id(),
                c.credentialName() != null ? c.credentialName() : "",
                c.transports() != null ? c.transports() : "",
                c.discoverable());
    }
}
