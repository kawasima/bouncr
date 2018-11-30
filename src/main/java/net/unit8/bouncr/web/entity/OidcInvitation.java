package net.unit8.bouncr.web.entity;

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
    private Invitation invitation;

    @OneToOne
    private OidcProvider oidcProvider;
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
