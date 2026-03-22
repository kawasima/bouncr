package net.unit8.bouncr.data;

import java.io.Serializable;

/**
 * Transient challenge data for a WebAuthn ceremony, stored in Redis.
 *
 * @param challenge random challenge bytes
 * @param userId    credential owner (null for discoverable-credential authentication)
 * @param rpId      relying party identifier
 * @param type      ceremony type: "registration" or "authentication"
 */
public record WebAuthnChallenge(
        byte[] challenge,
        Long userId,
        String rpId,
        String type
) implements Serializable {
    public static final String TYPE_REGISTRATION = "registration";
    public static final String TYPE_AUTHENTICATION = "authentication";
}
