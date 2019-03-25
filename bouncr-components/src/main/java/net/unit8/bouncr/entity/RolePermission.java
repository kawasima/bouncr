package net.unit8.bouncr.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "role_permissions")
public class RolePermission extends BaseFetchGroupTracker {
    @Id
    @OneToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @Id
    @OneToOne
    @JoinColumn(name = "permission_id")
    private Permission permission;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}
