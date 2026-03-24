package net.unit8.bouncr.api.boundary;

/**
 * Request body for updating a role.
 *
 * @param name role name
 * @param description human-readable description of the role
 */
public record RoleUpdate(String name, String description) {}
