package net.unit8.bouncr.data;

import java.util.List;

/**
 * Application managed by Bouncr.
 *
 * <p>An application groups realms and defines routing information for
 * gateway/proxy integration.
 *
 * @param id persistent identifier
 * @param name display name
 * @param description optional description
 * @param passTo upstream target URL
 * @param virtualPath external path prefix
 * @param topPage default landing page
 * @param writeProtected whether mutation is restricted
 * @param realms realms that belong to this application
 */
public record Application(Long id, String name, String description, String passTo, String virtualPath, String topPage, boolean writeProtected, List<Realm> realms) {

    /** Factory for decoder use — creates an Application without loaded relations. */
    public static Application of(Long id, String name, String description, String passTo, String virtualPath, String topPage, boolean writeProtected) {
        return new Application(id, name, description, passTo, virtualPath, topPage, writeProtected, null);
    }
}
