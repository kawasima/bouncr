package net.unit8.bouncr.data;

import java.util.Arrays;

/**
 * JWT signing key pair for an OIDC client application.
 *
 * <p>Defensively copies byte arrays on construction and access
 * to prevent mutation of key material.
 *
 * @param privateKey private key bytes
 * @param publicKey  public key bytes
 */
public record SigningKeyPair(byte[] privateKey, byte[] publicKey) {
    public SigningKeyPair {
        privateKey = privateKey != null ? privateKey.clone() : null;
        publicKey = publicKey != null ? publicKey.clone() : null;
    }

    @Override
    public byte[] privateKey() {
        return privateKey != null ? privateKey.clone() : null;
    }

    @Override
    public byte[] publicKey() {
        return publicKey != null ? publicKey.clone() : null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SigningKeyPair other
                && Arrays.equals(privateKey, other.privateKey)
                && Arrays.equals(publicKey, other.publicKey);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(privateKey) + Arrays.hashCode(publicKey);
    }

    @Override
    public String toString() {
        return "SigningKeyPair[privateKey=%d bytes, publicKey=%d bytes]".formatted(
                privateKey != null ? privateKey.length : 0,
                publicKey != null ? publicKey.length : 0);
    }
}
