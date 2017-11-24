package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;

/**
 * The entity of groups.
 *
 * @author kawasima
 */
@Entity
@Table(name = "groups")
@Data
@EqualsAndHashCode
public class Group implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String name;
    private String description;
    private Boolean writeProtected;
}
