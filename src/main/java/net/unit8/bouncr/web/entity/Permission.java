package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;

/**
 * The entity of permissions.
 *
 * @author kawasima
 */
@Entity
@Table(name = "permissions")
@Data
@EqualsAndHashCode
public class Permission implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    private String name;
    private String description;
    private Boolean writeProtected;
}
