package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.UnreachableException;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import net.unit8.bouncr.component.config.CertConfiguration;
import net.unit8.bouncr.component.config.OidcConfiguration;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.component.config.VerificationPolicy;
import net.unit8.bouncr.hook.HookRepository;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

public class BouncrConfiguration extends SystemComponent<BouncrConfiguration> {
    private Clock clock = Clock.systemDefaultZone();
    private boolean passwordEnabled = true;
    private boolean signUpEnabled = true;
    private long tokenExpires = 1800L;
    private long accessTokenExpires = 900L;
    private long refreshTokenExpires = 604800L;
    private long authorizationCodeExpires = 60L;
    private long oidcSessionExpires = 180L;
    private String tokenName = "BOUNCR_TOKEN";
    private String backendHeaderName = "X-Bouncr-Credential";
    private boolean secureCookie = true;
    private String issuerBaseUrl = "http://localhost:3000";
    private long idTokenExpires = 3600L;
    private long oauth2RefreshTokenExpires = 86400L;
    private byte[] keyEncryptionKey;
    private int pbkdf2Iterations = 10000;
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    private VerificationPolicy verificationPolicy = new VerificationPolicy();
    private CertConfiguration certConfiguration;
    private SecureRandom secureRandom;
    private MessageResource messageResource = new MessageResource(new HashSet<>(Arrays.asList(
            Locale.ENGLISH,
            Locale.JAPANESE))
    );
    private OidcConfiguration oidcConfiguration = new OidcConfiguration();

    private RetryPolicy<Object> httpClientRetryPolicy = RetryPolicy.builder()
            .handle(HttpTimeoutException.class)
            .handle(HttpConnectTimeoutException.class)
            .withBackoff(3, 10, ChronoUnit.SECONDS)
            .build();
    private CircuitBreaker<Object> ldapClientCircuitBreaker = CircuitBreaker.builder()
            .withFailureThreshold(5)
            .withSuccessThreshold(3)
            .withDelay(Duration.ofSeconds(5))
            .handle(NamingException.class)
            .build();
    private RetryPolicy<Object> ldapRetryPolicy = RetryPolicy.builder()
            .handle(CommunicationException.class)
            .withBackoff(3, 10, ChronoUnit.SECONDS)
            .build();

    private HookRepository hookRepo = new HookRepository();

    @Override
    protected ComponentLifecycle<BouncrConfiguration> lifecycle() {
        return new ComponentLifecycle<>() {
            @Override
            public void start(BouncrConfiguration component) {
                component.certConfiguration = new CertConfiguration();
                if (component.secureRandom == null) {
                    try {
                        component.secureRandom = SecureRandom.getInstance("NativePRNG");
                    } catch (NoSuchAlgorithmException e) {
                        try {
                            component.secureRandom = SecureRandom.getInstanceStrong();
                        } catch (NoSuchAlgorithmException algoEx) {
                            throw new UnreachableException(algoEx);
                        }
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

    public boolean isSignUpEnabled() {
        return signUpEnabled;
    }

    public void setSignUpEnabled(boolean signUpEnabled) {
        this.signUpEnabled = signUpEnabled;
    }

    public long getTokenExpires() {
        return tokenExpires;
    }

    public void setTokenExpires(long tokenExpires) {
        this.tokenExpires = tokenExpires;
    }

    public long getAccessTokenExpires() {
        return accessTokenExpires;
    }

    public void setAccessTokenExpires(long accessTokenExpires) {
        this.accessTokenExpires = accessTokenExpires;
    }

    public long getRefreshTokenExpires() {
        return refreshTokenExpires;
    }

    public void setRefreshTokenExpires(long refreshTokenExpires) {
        this.refreshTokenExpires = refreshTokenExpires;
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

    public boolean isSecureCookie() {
        return secureCookie;
    }

    public void setSecureCookie(boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public String getIssuerBaseUrl() {
        return issuerBaseUrl;
    }

    public void setIssuerBaseUrl(String issuerBaseUrl) {
        this.issuerBaseUrl = issuerBaseUrl;
    }

    public long getIdTokenExpires() {
        return idTokenExpires;
    }

    public void setIdTokenExpires(long idTokenExpires) {
        this.idTokenExpires = idTokenExpires;
    }

    public long getOauth2RefreshTokenExpires() {
        return oauth2RefreshTokenExpires;
    }

    public void setOauth2RefreshTokenExpires(long oauth2RefreshTokenExpires) {
        this.oauth2RefreshTokenExpires = oauth2RefreshTokenExpires;
    }

    public byte[] getKeyEncryptionKey() {
        return keyEncryptionKey;
    }

    public void setKeyEncryptionKey(byte[] keyEncryptionKey) {
        this.keyEncryptionKey = keyEncryptionKey;
    }

    public int getPbkdf2Iterations() {
        return pbkdf2Iterations;
    }

    public void setPbkdf2Iterations(int pbkdf2Iterations) {
        this.pbkdf2Iterations = pbkdf2Iterations;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public VerificationPolicy getVerificationPolicy() {
        return verificationPolicy;
    }

    public void setVerificationPolicy(VerificationPolicy verificationPolicy) {
        this.verificationPolicy = verificationPolicy;
    }

    public RetryPolicy<Object> getHttpClientRetryPolicy() {
        return httpClientRetryPolicy;
    }

    public void setHttpClientRetryPolicy(RetryPolicy<Object> httpClientRetryPolicy) {
        this.httpClientRetryPolicy = httpClientRetryPolicy;
    }

    public CircuitBreaker<Object> getLdapClientCircuitBreaker() {
        return ldapClientCircuitBreaker;
    }

    public void setLdapClientCircuitBreaker(CircuitBreaker<Object> ldapClientCircuitBreaker) {
        this.ldapClientCircuitBreaker = ldapClientCircuitBreaker;
    }

    public RetryPolicy<Object> getLdapRetryPolicy() {
        return ldapRetryPolicy;
    }

    public void setLdapRetryPolicy(RetryPolicy<Object> ldapRetryPolicy) {
        this.ldapRetryPolicy = ldapRetryPolicy;
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

    public HookRepository getHookRepo() {
        return hookRepo;
    }

    public OidcConfiguration getOidcConfiguration() {
        return oidcConfiguration;
    }

    public void setOidcConfiguration(OidcConfiguration oidcConfiguration) {
        this.oidcConfiguration = oidcConfiguration;
    }
}
