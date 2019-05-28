package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    @JsonIgnore
    @Column(name = "name_lower")
    private String nameLower;
    private String url;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", referencedColumnName = "application_id")
    @JsonBackReference("realms")
    private Application application;

    @JsonIgnore
    @Column(name = "write_protected")
    private Boolean writeProtected;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "realm")
    private List<Assignment> assignments;

    @JsonIgnore
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
        this.nameLower = Optional.ofNullable(name)
                .map(n -> n.toLowerCase(Locale.US))
                .orElse(null);
    }

    public String getNameLower() {
        return nameLower;
    }

    public void setNameLower(String nameLower) {
        this.nameLower = nameLower;
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

    @Override
    public String toString() {
        return "Realm{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", application=" + application +
                ", writeProtected=" + writeProtected +
                ", assignments=" + assignments +
                ", pathPattern=" + pathPattern +
                '}';
    }
}
