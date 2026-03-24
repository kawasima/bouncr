package net.unit8.bouncr.api.boundary;

/**
 * Request body for user self-registration.
 *
 * @param account desired user account name
 * @param code invitation code authorizing the sign-up
 * @param enablePasswordCredential whether to create a password credential during registration
 */
public record SignUp(String account, String code, boolean enablePasswordCredential) {}
