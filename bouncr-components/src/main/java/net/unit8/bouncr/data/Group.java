package net.unit8.bouncr.data;

import java.util.*;

/**
 * User group used for role assignment.
 *
 * @param id persistent identifier
 * @param name group name
 * @param description optional description
 * @param writeProtected whether mutation is restricted
 * @param users users currently in the group
 */
public record Group(Long id, String name, String description, Boolean writeProtected, List<User> users) {
}
