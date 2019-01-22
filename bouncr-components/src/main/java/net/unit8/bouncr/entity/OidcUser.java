package net.unit8.bouncr.entity;

import javax.persistence.*;

@Entity
@Table(name ="oidc_users")
public class OidcUser {
    @ManyToOne
    @JoinColumn(name = "oidc_provider_id")
    private OidcProvider oidcProvider;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "oidc_sub")
    private String oidcSub;

    public OidcProvider getOidcProvider() {
        return oidcProvider;
    }

    public void setOidcProvider(OidcProvider oidcProvider) {
        this.oidcProvider = oidcProvider;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOidcSub() {
        return oidcSub;
    }

    public void setOidcSub(String oidcSub) {
        this.oidcSub = oidcSub;
    }
}
