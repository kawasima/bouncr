package net.unit8.bouncr.api.boundary;

import java.util.List;
import java.util.Map;

/**
 * WebAuthn PublicKeyCredentialCreationOptions returned to the client
 * for credential registration.
 */
public record WebAuthnRegistrationOptions(
        String challenge,
        RelyingParty rp,
        UserEntity user,
        List<PubKeyCredParam> pubKeyCredParams,
        List<CredentialDescriptor> excludeCredentials,
        Map<String, String> authenticatorSelection,
        String attestation
) {
    public record RelyingParty(String id, String name) {}
    public record UserEntity(String id, String name, String displayName) {}
    public record PubKeyCredParam(String type, int alg) {}
    public record CredentialDescriptor(String type, String id) {}
}
