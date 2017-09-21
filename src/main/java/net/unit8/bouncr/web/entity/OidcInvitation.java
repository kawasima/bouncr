package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oidc_invitations")
@Data
public class OidcInvitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oidc_invitation_id")
    private Long id;

    private Long invitationId;

    private Long oidcProviderId;
    private String oidcSub;
}
