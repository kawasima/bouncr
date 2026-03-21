package net.unit8.bouncr.data;

/**
 * Value of a {@link UserProfileField} for a specific {@link User}.
 *
 * @param userProfileField profile field definition
 * @param user target user
 * @param value stored field value
 */
public record UserProfileValue(
    UserProfileField userProfileField,
    User user,
    String value
) {
}
