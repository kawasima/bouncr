package net.unit8.bouncr.data;

import java.util.*;

/**
 * The entity of groups.
 *
 * @author kawasima
 */
public record Group(Long id, String name, String description, Boolean writeProtected, List<User> users) {
}
