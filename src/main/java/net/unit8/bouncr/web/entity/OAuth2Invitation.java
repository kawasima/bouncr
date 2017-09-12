package net.unit8.bouncr.web.entity;

import lombok.Data;
import org.seasar.doma.*;

import java.io.Serializable;

@Entity
@Table(name = "oauth2_invitations")
@Data
public class OAuth2Invitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth2_invitation_id")
    private Long id;

    private Long invitationId;

    private Long oauth2ProviderId;
    private String oauth2UserName;
}
