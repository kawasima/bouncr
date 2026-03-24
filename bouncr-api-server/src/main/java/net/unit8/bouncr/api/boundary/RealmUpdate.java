package net.unit8.bouncr.api.boundary;

/**
 * Request body for updating a realm.
 *
 * @param name realm name
 * @param description human-readable description of the realm
 */
public record RealmUpdate(String name, String description) {}
