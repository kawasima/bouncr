package net.unit8.bouncr.data;

public record UserProfileValue(
    UserProfileField userProfileField,
    User user,
    String value
) {
}
