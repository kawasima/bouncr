package net.unit8.bouncr.api.boundary;

/**
 * Request body for creating a role.
 *
 * @param name role name
 * @param description human-readable description of the role
 */
public record RoleCreate(String name, String description) {}
