package net.unit8.bouncr.api.boundary;

import java.util.List;

/**
 * WebAuthn PublicKeyCredentialRequestOptions returned to the client
 * for credential authentication.
 */
public record WebAuthnAuthenticationOptions(
        String challenge,
        String rpId,
        List<AllowCredential> allowCredentials,
        String userVerification
) {
    public record AllowCredential(String type, String id, List<String> transports) {}
}
