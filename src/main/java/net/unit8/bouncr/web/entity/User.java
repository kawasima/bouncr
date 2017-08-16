package net.unit8.bouncr.web.entity;

import lombok.Data;
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
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    private String account;
    private String name;
    private String email;
}
