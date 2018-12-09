package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;
import org.eclipse.persistence.queries.FetchGroup;
import org.eclipse.persistence.queries.FetchGroupTracker;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * The entity of users.
 *
 * @author kawasima
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
        property  = "id",
        scope     = Long.class)
@Entity
@Table(name = "users")
public class User implements Serializable, FetchGroupTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String account;
    private String name;
    private String email;

    @JsonProperty("write_protected")
    @Column(name = "write_protected")
    private Boolean writeProtected;

    @ManyToMany
    @JoinTable(name = "memberships",
            joinColumns = @JoinColumn(name="user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private List<Group> groups;

    @OneToMany(mappedBy = "user")
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    private List<UserProfileValue> userProfileValues;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(Boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<UserProfileValue> getUserProfileValues() {
        return userProfileValues;
    }

    public void setUserProfileValues(List<UserProfileValue> userProfileValues) {
        this.userProfileValues = userProfileValues;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", writeProtected=" + writeProtected +
                ", groups=" + groups +
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
