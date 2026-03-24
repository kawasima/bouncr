package net.unit8.bouncr.api.boundary;

/**
 * Request body for creating a password credential.
 *
 * @param account user account name
 * @param password password to set
 * @param initial whether this is the initial password set during registration
 */
public record PasswordCredentialCreate(String account, String password, boolean initial) {}
