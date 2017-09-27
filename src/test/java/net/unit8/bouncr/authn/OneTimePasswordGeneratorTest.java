package net.unit8.bouncr.authn;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class OneTimePasswordGeneratorTest {
    @Test
    public void hotp() {
        byte[] key = "12345678901234567890".getBytes();
        OneTimePasswordGenerator gen = new OneTimePasswordGenerator(30);
        assertEquals(755224, gen.generateHotp(key, 0L));
        assertEquals(287082, gen.generateHotp(key, 1L));
        assertEquals(359152, gen.generateHotp(key, 2L));
        assertEquals(969429, gen.generateHotp(key, 3L));
        assertEquals(338314, gen.generateHotp(key, 4L));
        assertEquals(254676, gen.generateHotp(key, 5L));
        assertEquals(287922, gen.generateHotp(key, 6L));
        assertEquals(162583, gen.generateHotp(key, 7L));
        assertEquals(399871, gen.generateHotp(key, 8L));
        assertEquals(520489, gen.generateHotp(key, 9L));
    }
}
