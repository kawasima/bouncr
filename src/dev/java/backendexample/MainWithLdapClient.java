package backendexample;

import enkan.system.EnkanSystem;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;
import net.unit8.bouncr.BouncrEnkanSystem;
import net.unit8.bouncr.component.LdapClient;
import net.unit8.bouncr.ssl.BouncrSSLSocketFactory;

public class MainWithLdapClient {
    public static void main(String[] args) {
        EnkanSystem system = new BouncrEnkanSystem().create();
        system.setComponent("ldap", builder(new LdapClient())
                .set(LdapClient::setPort, 10636)
                .set(LdapClient::setScheme, "ldaps")
                .set(LdapClient::setSocketFactoryClassProvider, () -> {
                    BouncrSSLSocketFactory.setTruststorePath("src/dev/resources/bouncr.jks");
                    BouncrSSLSocketFactory.setTruststorePassword("password");
                    return BouncrSSLSocketFactory.class;
                })
                .build());
        system.relationships(
                component("ldap").using("config"),
                component("app").using("storeprovider", "datasource", "template", "doma", "jackson", "metrics",
                        "realmCache", "config", "jwt", "certificate", "trustManager", "ldap"));
        system.start();
    }
}
