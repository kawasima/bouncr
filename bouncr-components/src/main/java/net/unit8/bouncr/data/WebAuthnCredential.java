package net.unit8.bouncr.data;

/**
 * A WebAuthn (FIDO2) credential registered by a user.
 *
 * <p>Stores the serialized {@code AttestedCredentialData} bytes and metadata
 * needed to verify WebAuthn assertions during authentication.</p>
 *
 * @param id                      surrogate key
 * @param userId                  credential owner ID
 * @param credentialId            authenticator-assigned credential identifier (raw bytes)
 * @param attestedCredentialData  serialized {@code AttestedCredentialData} bytes (including the COSE public key)
 * @param signCount               monotonic counter for clone detection
 * @param transports              comma-separated transport hints (e.g. "internal,hybrid")
 * @param attestationFormat       attestation statement format (e.g. "none", "packed")
 * @param credentialName          user-assigned friendly name
 * @param discoverable            whether this is a discoverable credential (passkey)
 */
public record WebAuthnCredential(
        Long id,
        Long userId,
        byte[] credentialId,
        byte[] attestedCredentialData,
        long signCount,
        String transports,
        String attestationFormat,
        String credentialName,
        boolean discoverable
) {}
