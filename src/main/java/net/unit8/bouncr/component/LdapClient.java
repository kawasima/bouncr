package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.FalteringEnvironmentException;
import enkan.exception.MisconfigurationException;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.Properties;

public class LdapClient extends SystemComponent {
    private String host = "localhost";
    private int port = 389;
    private String user;
    private String password;
    private String searchBase;

    LdapContext ldapContext;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<LdapClient>() {
            @Override
            public void start(LdapClient component) {
                try {
                    Hashtable<String,String> env = new Hashtable<>();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                    env.put(Context.PROVIDER_URL, component.getLdapUrl());
                    env.put(Context.SECURITY_AUTHENTICATION, "simple");
                    env.put(Context.SECURITY_PRINCIPAL, component.user);
                    env.put(Context.SECURITY_CREDENTIALS, component.password);
                    component.ldapContext = new InitialLdapContext(env, null);
                } catch (NamingException e) {
                    // FIXME
                    throw new MisconfigurationException("", e);
                }
            }

            @Override
            public void stop(LdapClient component) {
                if (component.ldapContext != null) {
                    try {
                        component.ldapContext.close();
                    } catch (NamingException e) {
                        throw new FalteringEnvironmentException(e);
                    }
                }
            }
        };
    }

    public boolean search(String account, String password) {
        String searchFilter = "(sAMAccountName=" + account + ")";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> results = null;
        try {
            results = ldapContext.search(searchBase, searchFilter, searchControls);
            if (results.hasMore()) {
                SearchResult result = results.next();
                String distinguishedName = result.getNameInNamespace();
                Properties authEnv = new Properties();
                authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                authEnv.put(Context.PROVIDER_URL, getLdapUrl());
                authEnv.put(Context.SECURITY_PRINCIPAL, distinguishedName);
                authEnv.put(Context.SECURITY_CREDENTIALS, password);
                InitialDirContext ctx = new InitialDirContext(authEnv);
                return true;
            }
            return false;
        } catch (AuthenticationException e) {
            return false;
        } catch (NamingException e) {
            throw new MisconfigurationException("", e);
        } finally {
            if (results != null) {
                try {
                    while(results.hasMore());
                    results.close();
                } catch(NamingException ignore) {
                }
            }
        }

    }

    public String getLdapUrl() {
        return "ldap://" + host + ":" + port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }
}
