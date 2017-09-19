package net.unit8.bouncr.util;

import enkan.exception.MisconfigurationException;
import enkan.exception.UnreachableException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class PasswordUtils {
    public static byte[] pbkdf2(String password, String salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, 2048);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA384");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new UnreachableException(e);
        } catch (InvalidKeySpecException e) {
            throw new MisconfigurationException("core.INVALID_KEY", e);
        }
    }
}
