package net.unit8.bouncr.data;

/**
 * Group without loaded relations.
 *
 * @param id persistent identifier
 * @param groupSpec group specification
 * @param writeProtected whether mutation is restricted
 */
public record GroupPure(Long id, GroupSpec groupSpec, boolean writeProtected) implements Group {
    @Override public WordName name() { return groupSpec.name(); }
    @Override public String description() { return groupSpec.description(); }
}
