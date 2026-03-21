package net.unit8.bouncr.data;

import java.time.LocalDateTime;

/**
 * Current lock state for a user account.
 *
 * @param user locked user
 * @param lockLevel lock severity
 * @param lockedAt lock timestamp
 */
public record UserLock(
    User user,
    LockLevel lockLevel,
    LocalDateTime lockedAt
) {}
