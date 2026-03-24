package net.unit8.bouncr.api.boundary;

import java.util.List;

/**
 * Request body for updating a group.
 *
 * @param name group name
 * @param description human-readable description of the group
 * @param users list of user accounts to assign to the group
 */
public record GroupUpdate(String name, String description, List<String> users) {}
