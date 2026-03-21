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
    Boolean writeProtected,
    List<Role> roles
) {
}
