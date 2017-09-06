package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.Entity;

import java.io.Serializable;

@Entity
@Data
@EqualsAndHashCode
public class PermissionWithRealm implements Serializable {
    private Long realmId;
    private String permission;
}
