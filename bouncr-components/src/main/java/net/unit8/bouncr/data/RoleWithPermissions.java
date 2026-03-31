package net.unit8.bouncr.data;

import java.util.List;

/**
 * Role with loaded permissions.
 *
 * @param id persistent identifier
 * @param roleSpec role specification
 * @param writeProtected whether mutation is restricted
 * @param permissions permissions included in this role
 */
public record RoleWithPermissions(
    Long id,
    RoleSpec roleSpec,
    boolean writeProtected,
    List<Permission> permissions
) implements Role {
    @Override public WordName name() { return roleSpec.name(); }
    @Override public String description() { return roleSpec.description(); }

    public static RoleWithPermissions of(Role role, List<Permission> permissions) {
        RoleSpec spec = switch (role) {
            case RolePure p -> p.roleSpec();
            case RoleWithPermissions w -> w.roleSpec();
        };
        return new RoleWithPermissions(role.id(), spec, role.writeProtected(), permissions);
    }
}
