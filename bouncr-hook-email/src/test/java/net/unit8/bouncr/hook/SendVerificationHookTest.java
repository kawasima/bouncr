package net.unit8.bouncr.hook;

import enkan.component.thymeleaf.ThymeleafTemplateEngine;
import enkan.system.EnkanSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SendVerificationHookTest {
    private EnkanSystem system;

    @BeforeEach
    void setup() {
        system = EnkanSystem.of(
                "template", new ThymeleafTemplateEngine()
        );
        system.start();
    }

    @Test
    void test() {
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.stop();
        }
    }
}
