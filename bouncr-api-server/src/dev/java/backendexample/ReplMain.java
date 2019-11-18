package backendexample;

import enkan.system.command.MetricsCommandRegister;
import enkan.system.command.SqlCommand;
import enkan.system.repl.PseudoRepl;
import enkan.system.repl.ReplBoot;
import kotowari.system.KotowariCommandRegister;

/**
 * @author kawasima
 */
public class ReplMain {
    static {
        System.setProperty("hazelcast.jcache.provider.type", "client");
    }

    public static void main(String[] args) throws Exception {
        //JShellRepl repl = new JShellRepl("net.unit8.bouncr.api.BouncrApiEnkanSystemFactory");
        PseudoRepl repl = new PseudoRepl("net.unit8.bouncr.api.BouncrApiEnkanSystemFactory");
        ReplBoot.start(repl,
                new KotowariCommandRegister(),
                //new DevelCommandRegister(),
                new MetricsCommandRegister(),
                r -> {
                    r.registerCommand("sql", new SqlCommand());
                }
        );
    }
}
