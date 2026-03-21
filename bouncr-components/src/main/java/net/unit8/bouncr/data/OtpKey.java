package net.unit8.bouncr.data;

/**
 * One-time-password secret assigned to a user.
 *
 * @param user key owner
 * @param key raw secret bytes
 */
public record OtpKey(User user, byte[] key) {
}
