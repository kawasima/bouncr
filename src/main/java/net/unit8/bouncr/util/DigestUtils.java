package net.unit8.bouncr.util;

import enkan.exception.UnreachableException;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public class DigestUtils {

    private static final String HEX_CHARS = "0123456789abcdef";

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            int i = b & 0xff;
            sb.append(HEX_CHARS.charAt(i >> 4));
            sb.append(HEX_CHARS.charAt(i & 0xf));
        }
        return sb.toString();
    }
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
