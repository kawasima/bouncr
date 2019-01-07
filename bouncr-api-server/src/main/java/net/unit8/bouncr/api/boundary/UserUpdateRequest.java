package net.unit8.bouncr.api.boundary;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class UserUpdateRequest implements Serializable {
    private Map<String, Object> userProfiles = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getUserProfiles() {
        return userProfiles;
    }

    @JsonAnySetter
    public void setUserProfile(String name, Object value) {
        userProfiles.put(name, value);
    }

}
