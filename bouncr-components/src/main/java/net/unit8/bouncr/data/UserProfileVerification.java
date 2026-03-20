package net.unit8.bouncr.data;

import java.time.LocalDateTime;

public record UserProfileVerification(
    UserProfileVerificationId id,
    String code,
    LocalDateTime expiresAt
) {
}
