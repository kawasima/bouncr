package net.unit8.bouncr.data;

/**
 * Permission that can be granted to roles.
 *
 * @param id persistent identifier
 * @param permissionSpec permission specification
 * @param writeProtected whether mutation is restricted
 */
public record Permission(Long id, PermissionSpec permissionSpec, boolean writeProtected) {
    public PermissionName name() { return permissionSpec.name(); }
    public String description() { return permissionSpec.description(); }
}
