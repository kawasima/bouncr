package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.*;
import net.unit8.bouncr.json.IndirectListFilter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The entity of users.
 *
 * @author kawasima
 */
@Entity
@Table(name = "users")
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String account;

    @JsonIgnore
    @Column(name = "account_lower")
    private String accountLower;

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

    @JsonProperty("unverified_profiles")
    @Transient
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> unverifiedProfiles;

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
        this.accountLower = Optional.ofNullable(account)
                .map(acc -> acc.toLowerCase(Locale.US))
                .orElse(null);
    }

    public String getAccountLower() {
        return accountLower;
    }

    public void setAccountLower(String accountLower) {
        this.accountLower = accountLower;
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
        return Optional.ofNullable(this.userProfileValues).orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        u -> u.getUserProfileField().getJsonName(),
                        UserProfileValue::getValue
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

    public List<String> getUnverifiedProfiles() {
        return unverifiedProfiles;
    }

    public void setUnverifiedProfiles(List<String> unverifiedProfiles) {
        this.unverifiedProfiles = unverifiedProfiles;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        return Optional.ofNullable(obj)
                .filter(o -> getClass().isInstance(o))
                .map(o -> getClass().cast(o))
                .filter(o -> getId() != null && getId().equals(o.getId()))
                .isPresent();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
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
