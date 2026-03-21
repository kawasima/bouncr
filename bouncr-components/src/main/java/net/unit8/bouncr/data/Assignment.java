package net.unit8.bouncr.data;

/**
 * Triple relation binding a {@link Group}, {@link Role}, and {@link Realm}.
 *
 * <p>This is the core authorization mapping that grants permissions to users
 * through group membership in a specific realm.
 *
 * @param group target group
 * @param role granted role
 * @param realm realm where the role is effective
 */
public record Assignment(Group group, Role role, Realm realm) {
}
