package net.unit8.bouncr.component;

import is.tagomor.woothee.Classifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.quicktheories.quicktheories.QuickTheory.qt;
import static org.quicktheories.quicktheories.generators.SourceDSL.*;

public class UserAgentTest {
    @Test
    public void test() {
        Map<String, String> ua = Classifier.parse("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.52 Safari/537.36");
        assertEquals("Chrome", ua.get("name"));
    }

    @Test
    public void randomTest() {
        qt().forAll(strings().allPossible().ofLengthBetween(0, 1000))
                .check((s) -> {
                    Map<String, String> ua = Classifier.parse(s);
                    return Objects.equals("UNKNOWN", ua.get("name"));
                });
    }
}
