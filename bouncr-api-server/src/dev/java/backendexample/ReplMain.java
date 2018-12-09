package backendexample;

import enkan.system.command.MetricsCommandRegister;
import enkan.system.command.SqlCommand;
import enkan.system.devel.DevelCommandRegister;
import enkan.system.repl.PseudoRepl;
import enkan.system.repl.ReplBoot;
import enkan.system.repl.client.ReplClient;
import kotowari.system.KotowariCommandRegister;

/**
 * @author kawasima
 */
public class ReplMain {
    public static void main(String[] args) throws Exception {
        PseudoRepl repl = new PseudoRepl("net.unit8.bouncr.api.BouncrApiEnkanSystemFactory");
        ReplBoot.start(repl,
                new KotowariCommandRegister(),
                new DevelCommandRegister(),
                new MetricsCommandRegister(),
                r -> {
                    r.registerCommand("sql", new SqlCommand());
                }
        );

        new ReplClient().start(repl.getPort());

    }
}
