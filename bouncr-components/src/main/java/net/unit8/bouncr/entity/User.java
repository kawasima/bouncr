package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;
import net.unit8.bouncr.json.IndirectListFilter;
import org.eclipse.persistence.queries.FetchGroup;
import org.eclipse.persistence.queries.FetchGroupTracker;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The entity of users.
 *
 * @author kawasima
 */
@Entity
@Table(name = "users")
public class User implements Serializable, FetchGroupTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String account;

    @JsonIgnore
    @Column(name = "write_protected")
    private Boolean writeProtected;

    @ManyToMany
    @JoinTable(name = "memberships",
            joinColumns = @JoinColumn(name="user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IndirectListFilter.class)
    private List<Group> groups;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.ALL })
    @JsonIgnore
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

    @JsonAnyGetter
    public Map<String, Object> getUserProfiles() {
        return this.userProfileValues.stream()
                .collect(Collectors.toMap(
                        u -> u.getUserProfileField().getJsonName(),
                        u -> u.getValue()
                ));
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", writeProtected=" + writeProtected +
                ", groups=" + groups +
                '}';
    }

    @Transient
    private FetchGroup fetchGroup;
    @Transient
    private Session session;

    @Override
    public FetchGroup _persistence_getFetchGroup() {
        return fetchGroup;
    }

    @Override
    public void _persistence_setFetchGroup(FetchGroup group) {
        fetchGroup = group;
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
        return session;
    }

    @Override
    public void _persistence_setSession(Session session) {
        this.session = session;
    }
}
