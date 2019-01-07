package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;
import net.unit8.bouncr.json.IndirectListFilter;
import org.eclipse.persistence.indirection.IndirectList;
import org.eclipse.persistence.queries.FetchGroup;
import org.eclipse.persistence.queries.FetchGroupTracker;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.*;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * @author kawasima
 */
@Entity
@Table(name = "applications")
public class Application implements Serializable, FetchGroupTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    private String name;
    private String description;

    @JsonProperty("pass_to")
    @Column(name = "pass_to")
    private String passTo;

    @JsonProperty("virtual_path")
    @Column(name = "virtual_path")
    private String virtualPath;

    @JsonProperty("top_page")
    @Column(name = "top_page")
    private String topPage;

    @JsonIgnore
    @Column(name = "write_protected")
    private Boolean writeProtected;

    @OneToMany(mappedBy = "application")
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IndirectListFilter.class)
    @JsonManagedReference("realms")
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

    @Override
    public FetchGroup _persistence_getFetchGroup() {
        return null;
    }

    @Override
    public void _persistence_setFetchGroup(FetchGroup group) {

    }

    @Override
    public boolean _persistence_isAttributeFetched(String attribute) {
        return false;
    }

    @Override
    public void _persistence_resetFetchGroup() {

    }

    @Override
    public boolean _persistence_shouldRefreshFetchGroup() {
        return false;
    }

    @Override
    public void _persistence_setShouldRefreshFetchGroup(boolean shouldRefreshFetchGroup) {

    }

    @Override
    public Session _persistence_getSession() {
        return null;
    }

    @Override
    public void _persistence_setSession(Session session) {

    }
}
