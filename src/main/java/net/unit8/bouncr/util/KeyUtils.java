package net.unit8.bouncr.util;

import enkan.exception.MisconfigurationException;
import enkan.exception.UnreachableException;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class KeyUtils {
    public static KeyPair generate(int size) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(size, SecureRandom.getInstance("NativePRNGNonBlocking"));
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new UnreachableException(e);
        }
    }

    public static PrivateKey decode(byte[] encoded) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new UnreachableException(e);
        } catch (InvalidKeySpecException e) {
            throw new MisconfigurationException("core.INVALID_KEY", e);
        }
    }
}
