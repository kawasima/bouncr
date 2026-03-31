package net.unit8.bouncr.data;

/**
 * Flexible reference to an entity by id and/or name.
 *
 * <p>Used in API requests where callers may identify an entity by either
 * its persistent identifier or its name.
 *
 * @param id   entity identifier, may be null when only name is provided
 * @param name entity name, may be null when only id is provided
 */
public record EntityRef(Long id, String name) {
    /** Creates a reference by id only. */
    public static EntityRef ofId(Long id) {
        return new EntityRef(id, null);
    }
}
