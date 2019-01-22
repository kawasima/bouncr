package backendexample.cert;

import java.security.Provider;

public class BouncrProvider extends Provider {
    public BouncrProvider() {
        super("BOUNCR", "0.1", "Bouncr Provider");
    }
}
