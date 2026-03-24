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
    boolean writeProtected,
    List<Permission> permissions
) {
    /** Factory for decoder use — creates a Role without loaded relations. */
    public static Role of(Long id, String name, String description, boolean writeProtected) {
        return new Role(id, name, description, writeProtected, null);
    }
}
