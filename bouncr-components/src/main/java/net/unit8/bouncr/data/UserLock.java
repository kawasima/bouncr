package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Current lock state for a user account.
 *
 * @param lockLevel lock severity
 * @param lockedAt lock timestamp
 */
public record UserLock(
    LockLevel lockLevel,
    LocalDateTime lockedAt
) {}
