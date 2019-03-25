package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Map;

public class SignUpResponse implements Serializable {
    private Long id;

    private String account;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String password;

    private Map<String, Object> userProfiles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    @JsonAnyGetter
    public Map<String, Object> getUserProfiles() {
        return userProfiles;
    }

    public void setUserProfiles(Map<String, Object> userProfiles) {
        this.userProfiles = userProfiles;
    }
}
