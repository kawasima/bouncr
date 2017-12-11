package backendexample;

import enkan.component.ldaptive.LdapClient;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.BouncrEnkanSystem;
import org.ldaptive.ssl.KeyStoreCredentialConfig;
import org.ldaptive.ssl.SslConfig;

import static enkan.component.ComponentRelationship.*;
import static enkan.util.BeanBuilder.*;

public class MainWithLdapClient {
    public static void main(String[] args) {
        KeyStoreCredentialConfig keyStoreConfig = new KeyStoreCredentialConfig();
        keyStoreConfig.setTrustStore("file:./src/dev/resources/bouncr.jks");
        keyStoreConfig.setTrustStorePassword("password");


        EnkanSystem system = new BouncrEnkanSystem().create();
        system.setComponent("ldap", builder(new LdapClient())
                .set(LdapClient::setPort, 10636)
                .set(LdapClient::setScheme, "ldaps")
                .set(LdapClient::setSearchBase, "ou=users,dc=example,dc=com")
                .set(LdapClient::setSslConfig, new SslConfig(keyStoreConfig))
                .build());
        system.relationships(
                component("ldap").using("config"),
                component("app").using("storeprovider", "datasource", "template", "doma", "jackson", "metrics",
                        "realmCache", "config", "jwt", "certificate", "trustManager", "ldap"));
        system.start();
    }
}
