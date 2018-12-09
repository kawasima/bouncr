package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "oidc_applications")
public class OidcApplication implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oidc_application_id")
    private Long id;

    private String name;

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
}
