package net.unit8.bouncr.data;

/**
 * Specification for a role — the user-provided attributes without identity or system fields.
 *
 * @param name role name
 * @param description optional description
 */
public record RoleSpec(WordName name, String description) {}
