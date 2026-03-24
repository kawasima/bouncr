package net.unit8.bouncr.api.boundary;

/**
 * Request body for updating a permission.
 *
 * @param name permission name
 * @param description human-readable description of the permission
 */
public record PermissionUpdate(String name, String description) {}
