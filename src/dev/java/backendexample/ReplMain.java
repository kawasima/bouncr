package backendexample;

import enkan.system.command.MetricsCommandRegister;
import enkan.system.devel.DevelCommandRegister;
import enkan.system.repl.PseudoRepl;
import enkan.system.repl.ReplBoot;
import enkan.system.repl.pseudo.ReplClient;
import kotowari.system.KotowariCommandRegister;

/**
 * @author kawasima
 */
public class ReplMain {
    public static void main(String[] args) throws Exception {
        System.setProperty("ssl.port", "3002");
        System.setProperty("keystore", "src/dev/resources/keystore.jks");
        System.setProperty("key.password", "password");
        Class.forName("net.unit8.bouncr.authz.UserPermissionPrincipal");
        PseudoRepl repl = new PseudoRepl("net.unit8.bouncr.BouncrEnkanSystem");
        ReplBoot.start(repl,
                new KotowariCommandRegister(),
                new DevelCommandRegister(),
                new MetricsCommandRegister());

        new ReplClient().start(repl.getPort().get());

    }
}
