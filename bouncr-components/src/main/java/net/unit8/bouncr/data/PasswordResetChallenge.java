package net.unit8.bouncr.data;

import java.time.LocalDateTime;

public record PasswordResetChallenge(
    Long id,
    User user,
    String code,
    LocalDateTime expiresAt
) {}