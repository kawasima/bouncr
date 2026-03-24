package net.unit8.bouncr.data;

import java.util.List;

/**
 * Permission that can be granted to roles.
 *
 * @param id persistent identifier
 * @param name permission name
 * @param description optional description
 * @param writeProtected whether mutation is restricted
 * @param roles roles associated with this permission
 */
public record Permission(
    Long id,
    String name,
    String description,
    boolean writeProtected,
    List<Role> roles
) {
    /** Factory for decoder use — creates a Permission without loaded relations. */
    public static Permission of(Long id, String name, String description, boolean writeProtected) {
        return new Permission(id, name, description, writeProtected, null);
    }
}
