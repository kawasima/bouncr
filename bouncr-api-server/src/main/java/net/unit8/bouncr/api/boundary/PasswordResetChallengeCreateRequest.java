package net.unit8.bouncr.api.boundary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class PasswordResetChallengeCreateRequest implements Serializable {
    @NotBlank
    private String account;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
