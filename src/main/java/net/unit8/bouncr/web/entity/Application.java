package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

/**
 * @author kawasima
 */
@Entity
@Table(name = "APPLICATIONS")
@Data
public class Application {
    @Id
    @Column(name = "APPLICATION_ID")
    private Long id;

    private String name;
    private String description;
}
