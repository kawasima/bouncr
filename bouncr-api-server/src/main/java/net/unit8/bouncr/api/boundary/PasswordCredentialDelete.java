package net.unit8.bouncr.api.boundary;

/**
 * Request body for deleting a password credential.
 *
 * @param account user account name
 * @param password current password for verification
 */
public record PasswordCredentialDelete(String account, String password) {}
