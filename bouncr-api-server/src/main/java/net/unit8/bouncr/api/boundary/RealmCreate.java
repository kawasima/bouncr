package net.unit8.bouncr.api.boundary;

/**
 * Request body for creating a realm.
 *
 * @param name realm name
 * @param description human-readable description of the realm
 * @param url base URL associated with the realm
 */
public record RealmCreate(String name, String description, String url) {}
