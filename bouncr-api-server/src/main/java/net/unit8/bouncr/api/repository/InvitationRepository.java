package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.GroupInvitation;
import net.unit8.bouncr.data.Invitation;
import net.unit8.bouncr.data.OidcInvitation;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class InvitationRepository {
    private final DSLContext dsl;

    public InvitationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Invitation> findByCode(String code) {
        var rec = dsl.select(
                        field("invitation_id", Long.class),
                        field("code", String.class),
                        field("email", String.class),
                        field("invited_at", LocalDateTime.class))
                .from(table("invitations"))
                .where(field("code").eq(code))
                .fetchOne();
        if (rec == null) return Optional.empty();

        Invitation inv = INVITATION.decode(rec).getOrThrow();
        Long invitationId = inv.id();
        List<GroupInvitation> groupInvitations = findGroupInvitations(invitationId);
        List<OidcInvitation> oidcInvitations = findOidcInvitations(invitationId);

        return Optional.of(new Invitation(inv.id(), inv.code(), inv.email(), inv.invitedAt(),
                groupInvitations, oidcInvitations));
    }

    public Invitation insert(String email, String code, LocalDateTime invitedAt, List<Long> groupIds) {
        Record rec = dsl.insertInto(table("invitations"),
                        field("email"), field("code"), field("invited_at"))
                .values(email, code, invitedAt)
                .returningResult(
                        field("invitation_id", Long.class),
                        field("code", String.class),
                        field("email", String.class),
                        field("invited_at", LocalDateTime.class))
                .fetchOne();

        Invitation inv = INVITATION.decode(rec).getOrThrow();

        if (groupIds != null) {
            for (Long groupId : groupIds) {
                dsl.insertInto(table("group_invitations"),
                                field("invitation_id"), field("group_id"))
                        .values(inv.id(), groupId)
                        .execute();
            }
        }

        return inv;
    }

    public void delete(String code) {
        dsl.deleteFrom(table("invitations"))
                .where(field("code").eq(code))
                .execute();
    }

    public void insertOidcInvitation(Long invitationId, Long oidcProviderId, String oidcPayload) {
        dsl.insertInto(table("oidc_invitations"),
                        field("invitation_id"), field("oidc_provider_id"), field("oidc_payload"))
                .values(invitationId, oidcProviderId, oidcPayload)
                .execute();
    }

    private List<GroupInvitation> findGroupInvitations(Long invitationId) {
        return dsl.select(
                        field("gi.group_invitation_id", Long.class).as("group_invitation_id"),
                        field("g.group_id", Long.class).as("group_id"),
                        field("g.name", String.class).as("name"),
                        field("g.description", String.class).as("description"),
                        field("g.write_protected", Boolean.class).as("write_protected"))
                .from(table("group_invitations").as("gi"))
                .join(table("groups").as("g")).on(field("g.group_id").eq(field("gi.group_id")))
                .where(field("gi.invitation_id").eq(invitationId))
                .fetch(rec -> GROUP_INVITATION.decode(rec).getOrThrow());
    }

    private List<OidcInvitation> findOidcInvitations(Long invitationId) {
        return dsl.select(
                        field("oi.oidc_invitation_id", Long.class).as("oidc_invitation_id"),
                        field("oi.oidc_provider_id", Long.class).as("oidc_provider_id"),
                        field("oi.oidc_payload", String.class).as("oidc_payload"))
                .from(table("oidc_invitations").as("oi"))
                .where(field("oi.invitation_id").eq(invitationId))
                .fetch(rec -> OIDC_INVITATION.decode(rec).getOrThrow());
    }
}
