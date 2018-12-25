package net.unit8.bouncr.proxy;

import enkan.system.EnkanSystem;

public class Main {
    static {
        System.setProperty("hazelcast.jcache.provider.type", "client");
    }
    public static void main(String[] args) {
        final EnkanSystem system = new BouncrProxyEnkanSystemFactory().create();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> system.stop()));
        system.start();
    }
}
