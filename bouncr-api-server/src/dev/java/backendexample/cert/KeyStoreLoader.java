package backendexample.cert;

import java.security.KeyStore;
import java.security.KeyStoreException;

public class KeyStoreLoader {
    public static void main(String[] args) throws KeyStoreException {

        KeyStore keystore = KeyStore.getInstance("JKS", new BouncrProvider());
    }
}
