package net.unit8.bouncr.component;

import enkan.system.EnkanSystem;
import org.junit.Test;

import java.math.BigInteger;

public class FlakeTest {
    @Test
    public void test() {
        EnkanSystem system = EnkanSystem.of(
                "flake", new Flake()
        );

        system.start();

        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
        System.out.println(((Flake) system.getComponent("flake")).generateId());
    }
}
