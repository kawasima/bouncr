package net.unit8.bouncr.data;

/**
 * Realm without loaded relations.
 *
 * @param id persistent identifier
 * @param realmSpec realm specification
 * @param writeProtected whether mutation is restricted
 */
public record RealmPure(Long id, RealmSpec realmSpec, boolean writeProtected) implements Realm {
    @Override public WordName name() { return realmSpec.name(); }
    @Override public String url() { return realmSpec.url(); }
    @Override public String description() { return realmSpec.description(); }
}
