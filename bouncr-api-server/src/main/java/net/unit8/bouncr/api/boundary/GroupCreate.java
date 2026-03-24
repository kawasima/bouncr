package net.unit8.bouncr.api.boundary;

/**
 * Request body for creating a group.
 *
 * @param name group name
 * @param description human-readable description of the group
 */
public record GroupCreate(String name, String description) {}
