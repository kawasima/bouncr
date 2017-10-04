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
        //System.setProperty("ssl.port", "3002");
        //System.setProperty("keystore.path", "src/dev/resources/bouncr.jks");
        //System.setProperty("keystore.password", "password");
        //System.setProperty("truststore.path", "src/dev/resources/bouncr_clients.jks");
        //System.setProperty("truststore.password", "password");
        Class.forName("net.unit8.bouncr.authz.UserPermissionPrincipal");
        Class.forName("net.unit8.bouncr.sign.JwtHeader");
        Class.forName("net.unit8.bouncr.sign.JwtClaim");
        PseudoRepl repl = new PseudoRepl("net.unit8.bouncr.BouncrEnkanSystem");
        ReplBoot.start(repl,
                new KotowariCommandRegister(),
                new DevelCommandRegister(),
                new MetricsCommandRegister());

        new ReplClient().start(repl.getPort().get());

    }
}
