package net.unit8.bouncr.data;

/**
 * User group used for role assignment.
 */
public sealed interface Group permits GroupPure, GroupWithUsers {
    Long id();
    WordName name();
    String description();
    boolean writeProtected();
}
