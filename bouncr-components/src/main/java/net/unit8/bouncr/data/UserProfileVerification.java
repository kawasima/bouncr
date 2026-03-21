package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Verification challenge for a user profile field value.
 *
 * @param id composite identifier of user and field
 * @param code one-time verification code
 * @param expiresAt challenge expiration timestamp
 */
public record UserProfileVerification(
    UserProfileVerificationId id,
    String code,
    LocalDateTime expiresAt
) {
}
