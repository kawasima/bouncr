package net.unit8.bouncr.data;

import java.util.List;

/**
 * Role that groups permissions.
 *
 * @param id persistent identifier
 * @param name role name
 * @param description optional description
 * @param writeProtected whether mutation is restricted
 * @param permissions permissions included in this role
 */
public record Role(
    Long id,
    String name,
    String description,
    Boolean writeProtected,
    List<Permission> permissions
) {}
