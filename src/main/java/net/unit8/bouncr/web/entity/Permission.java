package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/**
 * The entity of permissions.
 *
 * @author kawasima
 */
@Entity
@Table(name = "PERMISSIONS")
@Data
public class Permission {
    @Id
    @Column(name = "PERMISSION_ID")
    private Long id;

    private String name;
    private String description;
}
