package net.unit8.bouncr.data;

/**
 * Composite key for {@link UserProfileVerification}.
 *
 * @param userProfileField profile field ID
 * @param user user ID
 */
public record UserProfileVerificationId(
    Long userProfileField,
    Long user) {
}
