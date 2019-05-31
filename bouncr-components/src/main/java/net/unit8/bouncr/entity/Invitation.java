package net.unit8.bouncr.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "invitations")
public class Invitation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;
    private String code;

    private String email;

    @JsonProperty("invited_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @OneToMany(mappedBy="invitation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<GroupInvitation> groupInvitations;

    @OneToMany(mappedBy="invitation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<OidcInvitation> oidcInvitations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public List<GroupInvitation> getGroupInvitations() {
        return groupInvitations;
    }

    public void setGroupInvitations(List<GroupInvitation> groupInvitations) {
        this.groupInvitations = groupInvitations;
    }

    public List<OidcInvitation> getOidcInvitations() {
        return oidcInvitations;
    }

    public void setOidcInvitations(List<OidcInvitation> oidcInvitations) {
        this.oidcInvitations = oidcInvitations;
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
