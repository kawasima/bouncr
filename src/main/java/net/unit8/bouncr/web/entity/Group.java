package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/**
 * The entity of groups.
 *
 * @author kawasima
 */
@Entity
@Table(name = "GROUPS")
@Data
public class Group {
    @Id
    @Column(name = "GROUP_ID")
    private Long id;

    private String name;
}
