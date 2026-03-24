package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Persisted password credential for a user.
 *
 * @param user credential owner
 * @param password password hash bytes
 * @param salt per-user salt
 * @param initial whether the password is an initial temporary password
 * @param createdAt creation timestamp
 */
public record PasswordCredential(
     User user,
     byte[] password,
     String salt,
     boolean initial,
     LocalDateTime createdAt) {

    /** Factory for decoder use — creates a PasswordCredential without user reference. */
    public static PasswordCredential of(byte[] password, String salt, boolean initial, LocalDateTime createdAt) {
        return new PasswordCredential(null, password, salt, initial, createdAt);
    }
}
