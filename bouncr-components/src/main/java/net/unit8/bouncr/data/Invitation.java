package net.unit8.bouncr.data;

import java.time.LocalDateTime;
import java.util.List;

public record Invitation(Long id, String code, String email, LocalDateTime invitedAt, List<GroupInvitation> groupInvitations, List<OidcInvitation> oidcInvitations) {
}
