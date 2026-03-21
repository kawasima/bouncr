package net.unit8.bouncr.data;

import java.util.List;

/**
 * Authorization boundary under an application.
 *
 * @param id persistent identifier
 * @param name display name
 * @param nameLower normalized lowercase name for lookup
 * @param url realm URL segment
 * @param description optional description
 * @param application owning application
 * @param writeProtected whether mutation is restricted
 * @param assignments group-role assignments for this realm
 */
public record Realm(
    Long id,
    String name,
    String nameLower,
    String url,
    String description,
    Application application,
    Boolean writeProtected,
    List<Assignment> assignments
) {
}
