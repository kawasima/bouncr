package net.unit8.bouncr.data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Invitation aggregate for onboarding users to groups and OIDC providers.
 *
 * @param id persistent identifier
 * @param code invitation code
 * @param email invited email address
 * @param invitedAt timestamp when the invitation was issued
 * @param groupInvitations linked group invitations
 * @param oidcInvitations linked OIDC invitations
 */
public record Invitation(Long id, String code, String email, LocalDateTime invitedAt, List<GroupInvitation> groupInvitations, List<OidcInvitation> oidcInvitations) {

    /** Factory for decoder use — creates an Invitation without loaded relations. */
    public static Invitation of(Long id, String code, String email, LocalDateTime invitedAt) {
        return new Invitation(id, code, email, invitedAt, null, null);
    }
}
