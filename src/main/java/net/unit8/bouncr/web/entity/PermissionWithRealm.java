package net.unit8.bouncr.web.entity;

import org.seasar.doma.Entity;

import java.io.Serializable;

@Entity
public class PermissionWithRealm implements Serializable {
    private Long realmId;
    private String permission;

    public Long getRealmId() {
        return realmId;
    }

    public void setRealmId(Long realmId) {
        this.realmId = realmId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
