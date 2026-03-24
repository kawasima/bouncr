package net.unit8.bouncr.api.boundary;

/**
 * Request body for password-based sign-in.
 *
 * @param account user account name
 * @param password user password
 * @param oneTimePassword optional TOTP code for two-factor authentication
 */
public record PasswordSignIn(String account, String password, String oneTimePassword) {}
