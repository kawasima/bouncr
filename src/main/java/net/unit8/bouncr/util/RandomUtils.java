package net.unit8.bouncr.util;

import java.util.Random;

public class RandomUtils {
    /**
     * Generate a random string.
     *
     * @param random the Random object
     * @param length the length of generated string
     * @return a generated random sting
     */
    public static String generateRandomString(Random random, int length) {
        return random.ints(48,122)
                .filter(i-> (i<57 || i>65) && (i <90 || i>97))
                .mapToObj(i -> (char) i)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
