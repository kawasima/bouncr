package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/**
 * The entity of users.
 *
 * @author kawasima
 */
@Entity
@Table(name = "USERS")
@Data
public class User {
    @Id
    @Column(name = "USER_ID")
    private Long id;
    private String name;
}
