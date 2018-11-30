package net.unit8.bouncr.web.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * @author kawasima
 */
@Entity
@Table(name = "realms")
public class Realm implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "realm_id")
    private Long id;

    private String name;
    private String url;
    private String description;

    @ManyToOne
    @JoinColumn(name = "application_id", referencedColumnName = "application_id")
    private Application application;

    @Column(name = "write_protected")
    private Boolean writeProtected;

    @Transient
    private transient Pattern pathPattern;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Boolean getWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(Boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    public Pattern getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(Pattern pathPattern) {
        this.pathPattern = pathPattern;
    }
}
