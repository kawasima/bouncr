package net.unit8.bouncr.component;

import is.tagomor.woothee.Classifier;
import org.junit.Test;

import java.util.Map;

public class UserAgentTest {
    @Test
    public void test() {
        Map<String, String> ua = Classifier.parse("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.52 Safari/537.36");
        System.out.println(ua);
    }
}
