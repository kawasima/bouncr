package net.unit8.bouncr.data;

/**
 * Specification for a group — the user-provided attributes without identity or system fields.
 *
 * @param name group name
 * @param description optional description
 */
public record GroupSpec(WordName name, String description) {}
