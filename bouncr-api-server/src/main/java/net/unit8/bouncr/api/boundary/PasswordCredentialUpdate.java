package net.unit8.bouncr.api.boundary;

/**
 * Request body for changing a password.
 *
 * @param account user account name
 * @param oldPassword current password for verification
 * @param newPassword new password to set
 */
public record PasswordCredentialUpdate(String account, String oldPassword, String newPassword) {}
