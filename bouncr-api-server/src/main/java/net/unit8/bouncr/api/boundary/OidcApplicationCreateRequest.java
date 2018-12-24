package net.unit8.bouncr.api.boundary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

public class OidcApplicationCreateRequest implements Serializable {
    @NotBlank
    private String name;

    @NotBlank
    private String homeUrl;

    @NotBlank
    private String callbackUrl;

    @NotBlank
    private String description;

    private List<Long> permissionId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Long> getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(List<Long> permissionId) {
        this.permissionId = permissionId;
    }
}
