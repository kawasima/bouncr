package backendexample;

import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.BouncrDevEnkanSystemFactory;

public class ReplMain {
    public static void main(String[] args) throws Exception {
        EnkanSystem system = new BouncrDevEnkanSystemFactory().create();
        system.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            system.stop();
        }, "shutdown-hook"));

        System.out.println("Server started. Press Ctrl-C to stop.");
        Thread.currentThread().join();
    }
}
