package net.unit8.bouncr.data;

/**
 * User with authentication credentials for sign-in flows.
 * Contains only the fields needed for password/WebAuthn authentication.
 */
public record UserCredentials(
    Long id,
    String account,
    PasswordCredential passwordCredential,
    OtpKey otpKey,
    UserLock userLock
) {}
