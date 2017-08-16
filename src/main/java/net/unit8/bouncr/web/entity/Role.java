package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

/**
 * @author kawasima
 */
@Entity
@Table(name = "ROLES")
@Data
public class Role implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_ID")
    private Long id;

    private String name;
    private String description;
}
