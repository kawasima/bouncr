package net.unit8.bouncr.data;

/**
 * Specification for a realm — the user-provided attributes without identity or system fields.
 *
 * @param name realm name
 * @param url realm URL segment
 * @param description optional description
 */
public record RealmSpec(WordName name, String url, String description) {}
