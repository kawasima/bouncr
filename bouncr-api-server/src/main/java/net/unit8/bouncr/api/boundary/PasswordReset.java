package net.unit8.bouncr.api.boundary;

/**
 * Request body for completing a password reset with a code.
 *
 * @param code verification code received via the password reset challenge
 */
public record PasswordReset(String code) {}
