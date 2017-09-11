package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;

public class BouncrConfiguration extends SystemComponent {
    private boolean passwordEnabled = true;
    private long tokenExpires = 1800L;
    private String tokenName = "BOUNCR_TOKEN";
    private String idHeaderName = "X-Bouncr-Id";
    private String permissionHeaderName = "X-Bouncr-Permissions";
    private PasswordPolicy passwordPolicy = new PasswordPolicy();


    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<BouncrConfiguration>() {
            @Override
            public void start(BouncrConfiguration component) {

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
}
