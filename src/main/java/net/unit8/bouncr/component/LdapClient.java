package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.FalteringEnvironmentException;
import enkan.exception.MisconfigurationException;
import net.jodah.failsafe.Failsafe;

import javax.naming.*;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.net.SocketFactory;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import static enkan.util.ThreadingUtils.some;

public class LdapClient extends SystemComponent {
    private String host = "localhost";
    private int port = 389;
    private String scheme = "ldap";
    private String user;
    private String password;
    private String searchBase;
    private String accountAttribute = "sAMAccountName";
    private AuthMethod authMethod = AuthMethod.NONE;
    private BouncrConfiguration config;
    private Supplier<Class<? extends SocketFactory>> socketFactoryClassProvider;

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
                    env.put(Context.SECURITY_AUTHENTICATION, authMethod.getValue());
                    if (authMethod != AuthMethod.NONE) {
                        env.put(Context.SECURITY_PRINCIPAL, component.user);
                        env.put(Context.SECURITY_CREDENTIALS, component.password);
                    }
                    if (Objects.equals(component.scheme, "ldaps")) {
                        env.put(Context.SECURITY_PROTOCOL, "ssl");
                        env.put("java.naming.ldap.factory.socket", component.socketFactoryClassProvider.get().getName());
                    }
                    component.ldapContext = new InitialLdapContext(env, null);
                } catch (NamingException e) {
                    throw new MisconfigurationException("ldap.CANNOT_CONNECT_TO_SERVER", e);
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
        String searchFilter = "(" + accountAttribute + "=" + account + ")";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        return Failsafe
                .with(config.getLdapClientCircuitBreaker())
                .with(config.getLdapRetryPolicy())
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
                            if (Objects.equals(scheme, "ldaps")) {
                                authEnv.put(Context.SECURITY_PROTOCOL, "ssl");
                                authEnv.put("java.naming.ldap.factory.socket", socketFactoryClassProvider.get().getName());
                            }

                            InitialDirContext authCtx = null;
                            try {
                                authCtx = new InitialDirContext(authEnv);
                                return true;
                            } finally {
                                if (authCtx != null) authCtx.close();
                            }
                        }
                        return false;
                    } catch (CommunicationException e) {
                        ldapContext.reconnect(null);
                        throw e;
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

    public void setSocketFactoryClassProvider(Supplier<Class<? extends SocketFactory>> socketFactoryClassProvider) {
        this.socketFactoryClassProvider = socketFactoryClassProvider;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public void setAccountAttribute(String accountAttribute) {
        this.accountAttribute = accountAttribute;
    }

    public enum AuthMethod {
        NONE("none"),
        SIMPLE("simple");

        private final String value;
        AuthMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
