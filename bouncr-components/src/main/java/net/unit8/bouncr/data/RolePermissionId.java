package net.unit8.bouncr.data;

/**
 * Composite key for {@link RolePermission}.
 *
 * @param role role ID
 * @param permission permission ID
 */
public record RolePermissionId(
    Long role,
    Long permission
) {}
