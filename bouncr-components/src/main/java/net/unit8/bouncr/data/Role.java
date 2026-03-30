package net.unit8.bouncr.data;

/**
 * Role that groups permissions.
 */
public sealed interface Role permits RolePure, RoleWithPermissions {
    Long id();
    WordName name();
    String description();
    boolean writeProtected();
}
