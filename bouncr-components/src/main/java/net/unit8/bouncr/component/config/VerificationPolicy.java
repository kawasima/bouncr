package net.unit8.bouncr.component.config;

import java.io.Serializable;

public class VerificationPolicy implements Serializable {
    private boolean verificationEnabledAtCreateUser = true;

    public boolean isVerificationEnabledAtCreateUser() {
        return verificationEnabledAtCreateUser;
    }

    public void setVerificationEnabledAtCreateUser(boolean verificationEnabledAtCreateUser) {
        this.verificationEnabledAtCreateUser = verificationEnabledAtCreateUser;
    }
}
