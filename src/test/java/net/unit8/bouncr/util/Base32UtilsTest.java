package net.unit8.bouncr.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base32UtilsTest {
    @Test
    public void test() {
        assertEquals("MY", Base32Utils.encode("f".getBytes()));
        assertEquals("MZXQ", Base32Utils.encode("fo".getBytes()));
        assertEquals("MZXW6", Base32Utils.encode("foo".getBytes()));
        assertEquals("MZXW6YQ", Base32Utils.encode("foob".getBytes()));
        assertEquals("MZXW6YTB", Base32Utils.encode("fooba".getBytes()));
        assertEquals("MZXW6YTBOI", Base32Utils.encode("foobar".getBytes()));
    }
}
