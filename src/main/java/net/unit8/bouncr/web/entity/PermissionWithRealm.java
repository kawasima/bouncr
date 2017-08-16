package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Entity;

import java.io.Serializable;

@Entity
@Data
public class PermissionWithRealm implements Serializable {
    private Long realmId;
    private String permission;
}
