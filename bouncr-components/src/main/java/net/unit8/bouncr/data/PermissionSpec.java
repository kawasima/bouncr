package net.unit8.bouncr.data;

/**
 * Specification for a permission — the user-provided attributes without identity or system fields.
 *
 * @param name permission name
 * @param description optional description
 */
public record PermissionSpec(PermissionName name, String description) {}
