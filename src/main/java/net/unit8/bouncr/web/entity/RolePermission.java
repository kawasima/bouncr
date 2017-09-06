package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.io.Serializable;

@Entity
@Table(name = "role_permissions")
@Data
@EqualsAndHashCode
public class RolePermission implements Serializable {
    @Id
    private Long roleId;
    @Id
    private Long permissionId;
}
