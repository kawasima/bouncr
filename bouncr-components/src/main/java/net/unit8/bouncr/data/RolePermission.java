package net.unit8.bouncr.data;

/**
 * Join model between {@link Role} and {@link Permission}.
 *
 * @param role role side
 * @param permission permission side
 */
public record RolePermission(
    Role role,
    Permission permission
) {}
