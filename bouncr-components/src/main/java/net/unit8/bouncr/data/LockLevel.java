package net.unit8.bouncr.data;

/**
 * Lock severity applied to a user account.
 */
public enum LockLevel {
    /** Temporary or lower-severity lock. */
    LOOSE,
    /** Hard lock that bans sign-in. */
    BAN
}
