package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SignUpCreateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @Pattern(regexp = "^\\w+$")
    private String account;
    private String code;

    @JsonProperty("enable_password_credential")
    private boolean enablePasswordCredential = true;

    private Map<String, Object> userProfiles = new HashMap<>();

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isEnablePasswordCredential() {
        return enablePasswordCredential;
    }

    public void setEnablePasswordCredential(boolean enablePasswordCredential) {
        this.enablePasswordCredential = enablePasswordCredential;
    }

    @JsonAnyGetter
    public Map<String, Object> getUserProfiles() {
        return userProfiles;
    }

    @JsonAnySetter
    public void setUserProfile(String name, Object value) {
        userProfiles.put(name, value);
    }

}
