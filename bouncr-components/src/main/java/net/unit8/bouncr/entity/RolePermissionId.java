package net.unit8.bouncr.entity;

import java.io.Serializable;
import java.util.Objects;

public class RolePermissionId implements Serializable {
    private Long role;
    private Long permission;

    public RolePermissionId() {}

    public RolePermissionId(Long role, Long permission) {
        this.role = role;
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePermissionId)) return false;
        RolePermissionId that = (RolePermissionId) o;
        return Objects.equals(role, that.role) &&
               Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, permission);
    }
}
