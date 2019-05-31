package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.unit8.bouncr.json.IndirectListFilter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "oidc_applications")
public class OidcApplication implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oidc_application_id")
    @JsonProperty("id")
    private Long id;

    private String name;
    @JsonIgnore
    @Column(name = "name_lower")
    private String nameLower;

    @JsonProperty("client_id")
    @Column(name = "client_id")
    private String clientId;

    @JsonProperty("client_secret")
    @Column(name = "client_secret")
    private String clientSecret;

    @JsonProperty("private_key")
    @Column(name = "private_key")
    private byte[] privateKey;

    @JsonProperty("public_key")
    @Column(name = "public_key")
    private byte[] publicKey;

    @JsonProperty("home_url")
    @Column(name = "home_url")
    private String homeUrl;

    @JsonProperty("callback_url")
    @Column(name = "callback_url")
    private String callbackUrl;
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "oidc_application_scopes",
            joinColumns = @JoinColumn(name="oidc_application_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IndirectListFilter.class)
    private List<Permission> permissions;

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
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
}
