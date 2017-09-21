package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.UnreachableException;
import net.unit8.bouncr.component.config.CertConfiguration;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class BouncrConfiguration extends SystemComponent {
    private boolean passwordEnabled = true;
    private long tokenExpires = 1800L;
    private long authorizationCodeExpires = 60L;
    private long oidcSessionExpires = 180L;
    private String tokenName = "BOUNCR_TOKEN";
    private String idHeaderName = "X-Bouncr-Id";
    private String permissionHeaderName = "X-Bouncr-Permissions";
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    private CertConfiguration certConfiguration;
    private SecureRandom secureRandom;

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

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getIdHeaderName() {
        return idHeaderName;
    }

    public void setIdHeaderName(String idHeaderName) {
        this.idHeaderName = idHeaderName;
    }

    public String getPermissionHeaderName() {
        return permissionHeaderName;
    }

    public void setPermissionHeaderName(String permissionHeaderName) {
        this.permissionHeaderName = permissionHeaderName;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
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
}
