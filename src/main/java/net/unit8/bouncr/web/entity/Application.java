package net.unit8.bouncr.web.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * @author kawasima
 */
@Entity
@Table(name = "applications")
public class Application implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    private String name;
    private String description;

    @Column(name = "pass_to")
    private String passTo;

    @Column(name = "virtual_path")
    private String virtualPath;

    @Column(name = "top_page")
    private String topPage;

    @Column(name = "write_protected")
    private Boolean writeProtected;

    private List<Realm> realms;

    public URI getUriToPass() {
        return URI.create(passTo);
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPassTo() {
        return passTo;
    }

    public void setPassTo(String passTo) {
        this.passTo = passTo;
    }

    public String getVirtualPath() {
        return virtualPath;
    }

    public void setVirtualPath(String virtualPath) {
        this.virtualPath = virtualPath;
    }

    public String getTopPage() {
        return topPage;
    }

    public void setTopPage(String topPage) {
        this.topPage = topPage;
    }

    public Boolean getWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(Boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    public List<Realm> getRealms() {
        return realms;
    }

    public void setRealms(List<Realm> realms) {
        this.realms = realms;
    }
}
