package net.unit8.bouncr.api.boundary;

/**
 * Assignment with all references resolved to persistent identifiers.
 *
 * @param groupId resolved group identifier
 * @param roleId resolved role identifier
 * @param realmId resolved realm identifier
 */
public record ResolvedAssignment(Long groupId, Long roleId, Long realmId) {}
