package net.unit8.bouncr.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "group_invitations")
public class GroupInvitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_invitation_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "invitation_id")
    private Invitation invitation;

    @OneToOne
    @JoinColumn(name = "group_id")
    private Group group;

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

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
}
