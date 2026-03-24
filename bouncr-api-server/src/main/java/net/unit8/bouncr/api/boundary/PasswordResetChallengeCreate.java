package net.unit8.bouncr.api.boundary;

/**
 * Request body for initiating a password reset.
 *
 * @param account user account name to reset the password for
 */
public record PasswordResetChallengeCreate(String account) {}
