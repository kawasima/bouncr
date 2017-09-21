package net.unit8.bouncr.web.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.seasar.doma.*;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * @author kawasima
 */
@Entity
@Table(name = "REALMS")
@Data
@EqualsAndHashCode
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

    @Transient
    private transient Pattern pathPattern;
}
