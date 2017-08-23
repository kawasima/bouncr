package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;
import java.net.URI;

/**
 * @author kawasima
 */
@Entity
@Table(name = "APPLICATIONS")
@Data
public class Application implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPLICATION_ID")
    private Long id;

    private String name;
    private String description;
    private String passTo;
    private String virtualPath;
    private String topPage;
    private Boolean writeProtected;

    public URI getUriToPass() {
        return URI.create(passTo);
    }

}
