package net.unit8.bouncr.data;

/**
 * Unresolved assignment reference using flexible entity references.
 *
 * <p>Callers may specify group, role, and realm by id or name.
 * The API layer resolves these to persistent identifiers before
 * creating an {@link AssignmentId}.
 *
 * @param group reference to the group
 * @param role  reference to the role
 * @param realm reference to the realm
 */
public record AssignmentRef(EntityRef group, EntityRef role, EntityRef realm) {}
