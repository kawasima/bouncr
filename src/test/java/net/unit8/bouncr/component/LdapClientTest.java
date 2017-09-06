package net.unit8.bouncr.component;

import org.junit.Test;

import static enkan.util.BeanBuilder.builder;
import static org.junit.Assert.*;

public class LdapClientTest {
    @Test
    public void test() {
        LdapClient client = builder(new LdapClient())
                .set(LdapClient::setPort, 10389)
                .set(LdapClient::setUser, "uid=admin,ou=system")
                .set(LdapClient::setPassword, "secret")
                .set(LdapClient::setSearchBase, "ou=users,dc=example,dc=com")
                .build();
        client.lifecycle().start(client);

        assertFalse(client.search("kawasima", "password2"));
        assertTrue(client.search("kawasima", "password"));
    }
}
