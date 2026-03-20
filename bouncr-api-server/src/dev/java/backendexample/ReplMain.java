package backendexample;

import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;

public class ReplMain {
    public static void main(String[] args) throws Exception {
        EnkanSystem system = new BouncrApiEnkanSystemFactory().create();
        system.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            system.stop();
        }, "shutdown-hook"));

        System.out.println("Server started. Press Ctrl-C to stop.");
        Thread.currentThread().join();
    }
}
