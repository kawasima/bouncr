package net.unit8.bouncr.data;

/**
 * Authorization boundary under an application.
 */
public sealed interface Realm permits RealmPure, RealmWithAssignments {
    Long id();
    WordName name();
    String url();
    String description();
    boolean writeProtected();
}
