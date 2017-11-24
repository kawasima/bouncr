package net.unit8.bouncr.component;

import enkan.collection.Multimap;
import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.UnreachableException;
import lombok.Getter;
import lombok.Setter;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import net.unit8.bouncr.component.config.CertConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.component.config.KvsSettings;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.HookRepository;

import javax.naming.NamingException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BouncrConfiguration extends SystemComponent {
    private Clock clock = Clock.systemDefaultZone();
    private boolean passwordEnabled = true;
    private long tokenExpires = 1800L;
    private long authorizationCodeExpires = 60L;
    private long oidcSessionExpires = 180L;
    private String tokenName = "BOUNCR_TOKEN";
    private String backendHeaderName = "X-Bouncr-credential";
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    private CertConfiguration certConfiguration;
    @Getter
    @Setter
    private KvsSettings keyValueStoreSettings = new KvsSettings();
    private SecureRandom secureRandom;
    private MessageResource messageResource = new MessageResource(new HashSet<>(Arrays.asList(
            Locale.ENGLISH,
            Locale.JAPANESE))
    );
    private RetryPolicy httpClientRetryPolicy = new RetryPolicy()
            .retryOn(SocketTimeoutException.class)
            .withBackoff(3, 10, TimeUnit.SECONDS);
    private CircuitBreaker ldapClientCircuitBreaker = new CircuitBreaker()
            .withFailureThreshold(5)
            .withSuccessThreshold(3)
            .withTimeout(5, TimeUnit.SECONDS)
            .failOn(NamingException.class);

    @Getter
    private HookRepository hookRepo = new HookRepository();

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<BouncrConfiguration>() {
            @Override
            public void start(BouncrConfiguration component) {
                certConfiguration = new CertConfiguration();
                if (secureRandom == null) {
                    try {
                        secureRandom = SecureRandom.getInstance("NativePRNGNonBlocking");
                    } catch (NoSuchAlgorithmException e) {
                        throw new UnreachableException();
                    }
                }
            }

            @Override
            public void stop(BouncrConfiguration component) {

            }
        };
    }

    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public long getTokenExpires() {
        return tokenExpires;
    }

    public void setTokenExpires(long tokenExpires) {
        this.tokenExpires = tokenExpires;
    }

    public long getAuthorizationCodeExpires() {
        return authorizationCodeExpires;
    }

    public void setAuthorizationCodeExpires(long authorizationCodeExpires) {
        this.authorizationCodeExpires = authorizationCodeExpires;
    }

    public long getOidcSessionExpires() {
        return oidcSessionExpires;
    }

    public void setOidcSessionExpires(long oidcSessionExpires) {
        this.oidcSessionExpires = oidcSessionExpires;
    }

    public String getTokenName() {
        return tokenName;
    }

    /**
     * Set the bouncr token name.
     *
     * It's set to the cookie header.
     * The default value is "BOUNCR_TOKEN".
     *
     * @param tokenName the Bouncr token name
     */
    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getBackendHeaderName() {
        return backendHeaderName;
    }

    public void setBackendHeaderName(String backendHeaderName) {
        this.backendHeaderName = backendHeaderName;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public RetryPolicy getHttpClientRetryPolicy() {
        return httpClientRetryPolicy;
    }

    public void setHttpClientRetryPolicy(RetryPolicy httpClientRetryPolicy) {
        this.httpClientRetryPolicy = httpClientRetryPolicy;
    }

    public CircuitBreaker getLdapClientCircuitBreaker() {
        return ldapClientCircuitBreaker;
    }

    public void setLdapClientCircuitBreaker(CircuitBreaker ldapClientCircuitBreaker) {
        this.ldapClientCircuitBreaker = ldapClientCircuitBreaker;
    }

    public CertConfiguration getCertConfiguration() {
        return certConfiguration;
    }

    public void setCertConfiguration(CertConfiguration certConfiguration) {
        this.certConfiguration = certConfiguration;
    }

    public SecureRandom getSecureRandom() {
        return secureRandom;
    }

    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public MessageResource getMessageResource() {
        return messageResource;
    }

    public void setMessageResource(MessageResource messageResource) {
        this.messageResource = messageResource;
    }


}
