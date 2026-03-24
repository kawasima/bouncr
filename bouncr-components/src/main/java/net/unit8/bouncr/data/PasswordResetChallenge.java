package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Password reset challenge issued to a user.
 *
 * @param id persistent identifier
 * @param user target user
 * @param code one-time challenge code
 * @param expiresAt expiration timestamp
 */
public record PasswordResetChallenge(
    Long id,
    User user,
    String code,
    LocalDateTime expiresAt
) {
    /** Factory for decoder use — creates a PasswordResetChallenge without user reference. */
    public static PasswordResetChallenge of(Long id, String code, LocalDateTime expiresAt) {
        return new PasswordResetChallenge(id, null, code, expiresAt);
    }
}
