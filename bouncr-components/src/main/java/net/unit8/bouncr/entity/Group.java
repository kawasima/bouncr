package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;
import org.eclipse.persistence.queries.FetchGroup;
import org.eclipse.persistence.queries.FetchGroupTracker;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.*;
import javax.persistence.criteria.Fetch;
import java.io.Serializable;
import java.util.List;

/**
 * The entity of groups.
 *
 * @author kawasima
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
        property  = "id",
        scope     = Long.class)
@Entity
@Table(name = "groups")
public class Group implements Serializable, FetchGroupTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    private String name;
    private String description;

    @Column(name = "write_protected")
    private Boolean writeProtected;

    @ManyToMany
    @JoinTable(name = "memberships",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<User> users;

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

    public Boolean getWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(Boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", writeProtected=" + writeProtected +
                '}';
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
