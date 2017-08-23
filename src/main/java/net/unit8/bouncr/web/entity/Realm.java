package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

/**
 * @author kawasima
 */
@Entity
@Table(name = "REALMS")
@Data
public class Realm implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REALM_ID")
    private Long id;

    private String name;
    private String url;
    private String description;
    private Long applicationId;
    private Boolean writeProtected;
}
