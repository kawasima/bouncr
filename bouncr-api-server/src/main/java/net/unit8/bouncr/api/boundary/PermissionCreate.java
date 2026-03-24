package net.unit8.bouncr.api.boundary;

/**
 * Request body for creating a permission.
 *
 * @param name permission name
 * @param description human-readable description of the permission
 */
public record PermissionCreate(String name, String description) {}
