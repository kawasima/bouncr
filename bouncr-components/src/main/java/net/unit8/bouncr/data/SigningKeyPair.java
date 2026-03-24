package net.unit8.bouncr.data;

/**
 * JWT signing key pair for an OIDC client application.
 *
 * @param privateKey private key bytes
 * @param publicKey  public key bytes
 */
public record SigningKeyPair(byte[] privateKey, byte[] publicKey) {}
