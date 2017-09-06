package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;

/**
 * The entity of users.
 *
 * @author kawasima
 */
@Entity
@Table(name = "USERS")
@Data
@EqualsAndHashCode
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    private String account;
    private String name;
    private String email;
    private Boolean writeProtected;
}
