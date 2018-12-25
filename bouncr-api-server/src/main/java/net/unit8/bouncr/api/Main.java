package net.unit8.bouncr.api;

import enkan.system.EnkanSystem;

public class Main {
    public static void main(String[] args) {
        EnkanSystem system = new BouncrApiEnkanSystemFactory().create();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> system.stop()));
        system.start();
    }
}
