package net.unit8.bouncr.web.entity;

import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oidc_invitations")
public class OidcInvitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oidc_invitation_id")
    private Long id;

    private Long invitationId;

    private Long oidcProviderId;
    private String oidcPayload;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public Long getOidcProviderId() {
        return oidcProviderId;
    }

    public void setOidcProviderId(Long oidcProviderId) {
        this.oidcProviderId = oidcProviderId;
    }

    public String getOidcPayload() {
        return oidcPayload;
    }

    public void setOidcPayload(String oidcPayload) {
        this.oidcPayload = oidcPayload;
    }
}
