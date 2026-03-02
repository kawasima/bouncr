package backendexample;

import enkan.system.EnkanSystem;
import enkan.system.command.MetricsCommandRegister;
import enkan.system.command.SqlCommand;
import enkan.system.repl.PseudoRepl;
import enkan.system.repl.ReplBoot;
import kotowari.system.KotowariCommandRegister;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;

/**
 * Entry point for development.
 *
 * <p>Two startup modes are available:
 * <ul>
 *   <li><b>Direct mode</b> (default, no arguments) — starts {@link EnkanSystem} directly and
 *       blocks until the JVM receives a shutdown signal (Ctrl-C).</li>
 *   <li><b>REPL mode</b> ({@code --repl} argument) — launches a {@code PseudoRepl} with the
 *       standard kotowari commands plus {@code sql}. Useful for interactive development.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   mvn exec:java -Pdev,slf4j-simple                       # direct mode
 *   mvn exec:java -Pdev,slf4j-simple -Dexec.args="--repl"  # REPL mode
 * </pre>
 */
public class ReplMain {
    public static void main(String[] args) throws Exception {
        boolean replMode = args.length > 0 && "--repl".equals(args[0]);

        if (replMode) {
            startRepl();
        } else {
            startDirect();
        }
    }

    /**
     * Starts the EnkanSystem directly and registers a shutdown hook to stop it cleanly.
     * The main thread parks until the JVM shuts down (e.g. Ctrl-C).
     */
    private static void startDirect() throws InterruptedException {
        EnkanSystem system = new BouncrApiEnkanSystemFactory().create();
        system.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            system.stop();
        }, "shutdown-hook"));

        System.out.println("Server started. Press Ctrl-C to stop.");
        Thread.currentThread().join();
    }

    /**
     * Starts a PseudoRepl with kotowari commands and sql command.
     */
    private static void startRepl() throws Exception {
        PseudoRepl repl = new PseudoRepl("net.unit8.bouncr.api.BouncrApiEnkanSystemFactory");
        ReplBoot.start(repl,
                new KotowariCommandRegister(),
                new MetricsCommandRegister(),
                r -> r.registerCommand("sql", new SqlCommand())
        );
    }
}
