package net.unit8.bouncr.data;

/**
 * Composite key for an {@link Assignment}.
 *
 * @param group group ID
 * @param role role ID
 * @param realm realm ID
 */
public record AssignmentId(Long group, Long role, Long realm) {
}
