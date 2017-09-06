package net.unit8.bouncr;

import java.util.Random;

public class RandomUtils {
    public static String generateRandomString(Random random, int length) {
        return random.ints(48,122)
                .filter(i-> (i<57 || i>65) && (i <90 || i>97))
                .mapToObj(i -> (char) i)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
