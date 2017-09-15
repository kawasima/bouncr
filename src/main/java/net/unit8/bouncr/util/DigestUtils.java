package net.unit8.bouncr.util;

import enkan.exception.UnreachableException;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class DigestUtils {

    public static String md5hex(String text) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
            byte[] digest = md.digest();
            return DatatypeConverter
                    .printHexBinary(digest).toLowerCase(Locale.US);
        } catch (NoSuchAlgorithmException e) {
            throw new UnreachableException(e);
        }
    }
}
