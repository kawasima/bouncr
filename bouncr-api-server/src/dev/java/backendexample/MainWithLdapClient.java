package backendexample;

import enkan.component.ldaptive.LdapClient;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.BouncrApiEnkanSystemFactory;
import org.ldaptive.ssl.KeyStoreCredentialConfig;
import org.ldaptive.ssl.SslConfig;

import static enkan.component.ComponentRelationship.*;
import static enkan.util.BeanBuilder.*;

public class MainWithLdapClient {
    public static void main(String[] args) {
        KeyStoreCredentialConfig keyStoreConfig = new KeyStoreCredentialConfig();
        keyStoreConfig.setTrustStore("file:./src/dev/resources/bouncr.jks");
        keyStoreConfig.setTrustStorePassword("password");


        EnkanSystem system = new BouncrApiEnkanSystemFactory().create();
        system.setComponent("src/dev/resources/ldap", builder(new LdapClient())
                .set(LdapClient::setPort, 10636)
                .set(LdapClient::setScheme, "ldaps")
                .set(LdapClient::setSearchBase, "ou=users,dc=example,dc=com")
                .set(LdapClient::setSslConfig, new SslConfig(keyStoreConfig))
                .build());
        system.relationships(
                component("src/dev/resources/ldap").using("config"),
                component("app").using("storeprovider", "datasource", "template", "doma", "jackson", "metrics",
                        "realmCache", "config", "jwt", "certificate", "trustManager", "src/dev/resources/ldap"));
        system.start();
    }
}
