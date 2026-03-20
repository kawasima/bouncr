package net.unit8.bouncr.data;

import java.time.LocalDateTime;

public record UserLock(
    User user,
    LockLevel lockLevel,
    LocalDateTime lockedAt
) {}
