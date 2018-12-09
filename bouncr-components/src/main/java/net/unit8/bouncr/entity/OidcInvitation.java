package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "oidc_invitations")
public class OidcInvitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oidc_invitation_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "invitation_id")
    private Invitation invitation;

    @OneToOne
    @JoinColumn(name = "oidc_provider_id")
    @JsonProperty("oidc_provider")
    private OidcProvider oidcProvider;

    @JsonProperty("oidc_payload")
    @Column(name = "oidc_payload")
    private String oidcPayload;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invitation getInvitation() {
        return invitation;
    }

    public void setInvitation(Invitation invitation) {
        this.invitation = invitation;
    }

    public OidcProvider getOidcProvider() {
        return oidcProvider;
    }

    public void setOidcProvider(OidcProvider oidcProvider) {
        this.oidcProvider = oidcProvider;
    }

    public String getOidcPayload() {
        return oidcPayload;
    }

    public void setOidcPayload(String oidcPayload) {
        this.oidcPayload = oidcPayload;
    }
}
