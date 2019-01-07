package net.unit8.bouncr.api.boundary;

import java.io.Serializable;
import java.util.List;

public class RolePermissionsCreateRequest implements Serializable {
    private List<String> permissions;

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
