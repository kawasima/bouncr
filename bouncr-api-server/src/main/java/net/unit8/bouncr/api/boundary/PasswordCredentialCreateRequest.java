package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class PasswordCredentialCreateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @JsonProperty(value = "account", required = true)
    private String account;

    @NotBlank
    @JsonProperty(value = "password", required = true)
    private String password;

    @JsonProperty("initial")
    private boolean initial = true;

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

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }
}
