import enkan.system.EnkanSystem;
import net.unit8.bouncr.proxy.BouncrProxyEnkanSystemFactory;

public class HazelCastMain {
    static {
        System.setProperty("hazelcast.jcache.provider.type", "client");
    }
    public static void main(String[] args) {
        final EnkanSystem system = new BouncrProxyEnkanSystemFactory().create();
        Runtime.getRuntime().addShutdownHook(new Thread(system::stop));
        system.start();
    }
}
