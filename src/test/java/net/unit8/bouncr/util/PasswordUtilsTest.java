package net.unit8.bouncr.util;

import org.junit.Test;

public class PasswordUtilsTest {
    @Test
    public void test() {
        byte[] hash = PasswordUtils.pbkdf2("password", "salt", 100);
        System.out.println(hash.length);
    }
}
