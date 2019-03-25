package net.unit8.bouncr.component;

import enkan.system.EnkanSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class FlakeTest {
    private EnkanSystem system;

    @BeforeEach
    void setup() {
        system = EnkanSystem.of(
                "flake", new Flake()
        );

        system.start();
    }

    @Test
    void sortedOrder() {
        Flake flake = system.getComponent("flake");

        BigInteger[] ids = new BigInteger[100];
        for (int i=0; i<100; i++) {
            ids[i] = flake.generateId();
        }

        for (int i=0; i<99; i++) {
            assertThat(ids[i]).isLessThan(ids[i+1]);
        }
    }

    @AfterEach
    void tearDown() {
        system.stop();
        system = null;
    }
}
