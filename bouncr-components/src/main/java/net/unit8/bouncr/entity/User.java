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
public class User extends BaseFetchGroupTracker {
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

    @OneToOne(mappedBy = "user", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("lock")
    private UserLock userLock;

    @OneToOne(mappedBy = "user", cascade = { CascadeType.ALL },
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private PasswordCredential passwordCredential;

    @OneToOne(mappedBy = "user", cascade = { CascadeType.ALL },
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private OtpKey otpKey;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OidcUser> oidcUsers;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> permissions;

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

    public UserLock getUserLock() {
        return userLock;
    }

    public void setUserLock(UserLock userLock) {
        this.userLock = userLock;
    }

    public OtpKey getOtpKey() {
        return otpKey;
    }

    public void setOtpKey(OtpKey otpKey) {
        this.otpKey = otpKey;
    }

    public PasswordCredential getPasswordCredential() {
        return passwordCredential;
    }

    public void setPasswordCredential(PasswordCredential passwordCredential) {
        this.passwordCredential = passwordCredential;
    }

    public List<OidcUser> getOidcUsers() {
        return oidcUsers;
    }

    public void setOidcUsers(List<OidcUser> oidcUsers) {
        this.oidcUsers = oidcUsers;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
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
}
