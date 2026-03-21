package net.unit8.bouncr.data;

/**
 * Master data for a user action that can be audited.
 *
 * @param id persistent identifier
 * @param name action name (for example, {@code user.signin})
 */
public record Action(Long id, String name) {
}
