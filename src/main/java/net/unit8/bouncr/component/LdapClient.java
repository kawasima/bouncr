package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.FalteringEnvironmentException;
import enkan.exception.MisconfigurationException;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import org.bouncycastle.est.jcajce.SSLSocketFactoryCreator;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Properties;

public class LdapClient extends SystemComponent {
    private String host = "localhost";
    private int port = 389;
    private String scheme = "ldap";
    private String user;
    private String password;
    private String searchBase;
    private BouncrConfiguration config;
    private Class<? extends SocketFactory> socketFactoryClass;

    private LdapContext ldapContext;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<LdapClient>() {
            @Override
            public void start(LdapClient component) {
                component.config = getDependency(BouncrConfiguration.class);
                try {

                    Hashtable<String,String> env = new Hashtable<>();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                    env.put(Context.PROVIDER_URL, component.getLdapUrl());
                    env.put(Context.SECURITY_AUTHENTICATION, "simple");
                    env.put(Context.SECURITY_PRINCIPAL, component.user);
                    env.put(Context.SECURITY_CREDENTIALS, component.password);
                    if (Objects.equals(component.scheme, "ldaps")) {
                        env.put(Context.SECURITY_PROTOCOL, "ssl");
                        env.put("java.naming.ldap.factory.socket", component.socketFactoryClass.getName());
                    }
                    component.ldapContext = new InitialLdapContext(env, null);
                } catch (NamingException e) {
                    // FIXME
                    e.printStackTrace();
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

        return Failsafe.with(config.getLdapClientCircuitBreaker())
                .get(() -> {
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
                            new InitialDirContext(authEnv);
                            return true;
                        }
                        return false;
                    } catch (AuthenticationException e) {
                        return false;
                    } finally {
                        if (results != null) {
                            try {
                                while (results.hasMore()) ;
                                results.close();
                            } catch (NamingException ignore) {
                            }
                        }
                    }
                });
    }

    public String getLdapUrl() {
        return scheme + "://" + host + ":" + port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
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

    public void setSocketFactoryClass(Class<? extends SocketFactory> socketFactoryClass) {
        this.socketFactoryClass = socketFactoryClass;
    }
}
