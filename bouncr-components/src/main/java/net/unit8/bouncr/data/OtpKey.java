package net.unit8.bouncr.data;

/**
 * One-time-password secret assigned to a user.
 *
 * @param key raw secret bytes
 */
public record OtpKey(byte[] key) {
}
