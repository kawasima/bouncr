package net.unit8.bouncr.api.boundary;

/**
 * A single group-role-realm assignment tuple.
 *
 * @param group reference to the group being assigned
 * @param role reference to the role being assigned
 * @param realm reference to the realm being assigned
 */
public record AssignmentItem(AssignmentIdObject group, AssignmentIdObject role, AssignmentIdObject realm) {}
