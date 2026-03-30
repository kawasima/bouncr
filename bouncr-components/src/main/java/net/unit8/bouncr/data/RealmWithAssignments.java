package net.unit8.bouncr.data;

import java.util.List;

/**
 * Realm with loaded assignments.
 *
 * @param id persistent identifier
 * @param realmSpec realm specification
 * @param writeProtected whether mutation is restricted
 * @param assignments group-role assignments for this realm
 */
public record RealmWithAssignments(
    Long id,
    RealmSpec realmSpec,
    boolean writeProtected,
    List<Assignment> assignments
) implements Realm {
    @Override public WordName name() { return realmSpec.name(); }
    @Override public String url() { return realmSpec.url(); }
    @Override public String description() { return realmSpec.description(); }

    public static RealmWithAssignments of(Realm realm, List<Assignment> assignments) {
        RealmSpec spec = switch (realm) {
            case RealmPure p -> p.realmSpec();
            case RealmWithAssignments w -> w.realmSpec();
        };
        return new RealmWithAssignments(realm.id(), spec, realm.writeProtected(), assignments);
    }
}
