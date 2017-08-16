package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

/**
 * The entity of groups.
 *
 * @author kawasima
 */
@Entity
@Table(name = "GROUPS")
@Data
public class Group implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GROUP_ID")
    private Long id;

    private String name;
}
