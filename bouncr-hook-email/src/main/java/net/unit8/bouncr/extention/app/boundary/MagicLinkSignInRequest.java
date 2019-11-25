package net.unit8.bouncr.extention.app.boundary;

import org.hibernate.validator.constraints.Length;

public class MagicLinkSignInRequest {
    @Length(max = 255)
    private String email;

    @Length(max = 255)
    private String account;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
