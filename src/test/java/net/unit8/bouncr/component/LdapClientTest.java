package net.unit8.bouncr.component;

import enkan.system.EnkanSystem;
import org.junit.Test;

import static enkan.util.BeanBuilder.builder;
import static org.junit.Assert.*;

public class LdapClientTest {
    public void test() {
        EnkanSystem system = EnkanSystem.of("ldap", builder(new LdapClient())
                .set(LdapClient::setPort, 10389)
                .set(LdapClient::setUser, "uid=admin,ou=system")
                .set(LdapClient::setPassword, "secret")
                .set(LdapClient::setSearchBase, "ou=users,dc=example,dc=com")
                .build());

        system.start();
        LdapClient client = (LdapClient) system.getComponent("ldap");

        assertFalse(client.search("kawasima", "password2"));
        assertTrue(client.search("kawasima", "password"));
    }
}
