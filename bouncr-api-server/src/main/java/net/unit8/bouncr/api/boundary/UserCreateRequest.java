package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class UserCreateRequest implements Serializable {
    @NotBlank
    @Length(max = 100)
    @Pattern(regexp = "^\\w+$")
    private String account;

    private Map<String, Object> userProfiles = new HashMap<>();

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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
