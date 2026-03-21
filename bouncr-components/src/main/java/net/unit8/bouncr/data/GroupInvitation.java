package net.unit8.bouncr.data;

/**
 * Invitation entry that links an {@link Invitation} to a {@link Group}.
 *
 * @param id persistent identifier
 * @param invitation parent invitation
 * @param group invited group
 */
public record GroupInvitation(Long id, Invitation invitation, Group group) {
}
