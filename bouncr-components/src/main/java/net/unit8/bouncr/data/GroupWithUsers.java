package net.unit8.bouncr.data;

import java.util.List;

/**
 * Group with loaded users.
 *
 * @param id persistent identifier
 * @param groupSpec group specification
 * @param writeProtected whether mutation is restricted
 * @param users users currently in the group
 */
public record GroupWithUsers(
    Long id,
    GroupSpec groupSpec,
    boolean writeProtected,
    List<User> users
) implements Group {
    @Override public WordName name() { return groupSpec.name(); }
    @Override public String description() { return groupSpec.description(); }

    public static GroupWithUsers of(Group group, List<User> users) {
        GroupSpec spec = switch (group) {
            case GroupPure p -> p.groupSpec();
            case GroupWithUsers w -> w.groupSpec();
        };
        return new GroupWithUsers(group.id(), spec, group.writeProtected(), users);
    }
}
