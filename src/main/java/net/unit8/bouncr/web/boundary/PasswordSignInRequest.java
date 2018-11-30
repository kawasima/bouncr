package net.unit8.bouncr.web.boundary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class PasswordSignInRequest implements Serializable {
    @NotBlank
    private String account;

    @NotBlank
    private String password;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
