package net.unit8.bouncr.util;

import enkan.exception.UnreachableException;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomUtilsTest {
    @Test
    public void test() {
        AtomicInteger ai = new AtomicInteger();
        for (int i=0; i < 10000; i++) {
            try {
                SecureRandom random = SecureRandom.getInstance("NativePRNGNonBlocking");
                ai.set(random.nextInt());
            } catch (NoSuchAlgorithmException e) {
                throw new UnreachableException(e);
            }
        }
    }
}
