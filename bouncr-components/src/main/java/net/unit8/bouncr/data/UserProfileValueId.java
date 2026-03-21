package net.unit8.bouncr.data;

/**
 * Composite key for {@link UserProfileValue}.
 *
 * @param userProfileField profile field ID
 * @param user user ID
 */
public record UserProfileValueId(
    Long userProfileField,
    Long user
) { }
