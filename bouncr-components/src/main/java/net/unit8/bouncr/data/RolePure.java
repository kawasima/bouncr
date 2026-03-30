package net.unit8.bouncr.data;

/**
 * Role without loaded relations.
 *
 * @param id persistent identifier
 * @param roleSpec role specification
 * @param writeProtected whether mutation is restricted
 */
public record RolePure(Long id, RoleSpec roleSpec, boolean writeProtected) implements Role {
    @Override public WordName name() { return roleSpec.name(); }
    @Override public String description() { return roleSpec.description(); }
}
