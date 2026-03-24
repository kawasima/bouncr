package net.unit8.bouncr.api.boundary;

/**
 * Flexible reference to an entity by id or name.
 *
 * @param id entity identifier, used when available
 * @param name entity name, used as a fallback when id is absent
 */
public record AssignmentIdObject(Long id, String name) {}
