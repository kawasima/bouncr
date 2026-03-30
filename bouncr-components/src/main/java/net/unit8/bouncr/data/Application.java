package net.unit8.bouncr.data;

import java.util.List;

/**
 * Application managed by Bouncr.
 *
 * <p>An application groups realms and defines routing information for
 * gateway/proxy integration.
 *
 * @param id persistent identifier
 * @param applicationSpec application specification
 * @param writeProtected whether mutation is restricted
 * @param realms realms that belong to this application
 */
public sealed interface Application permits ApplicationWithRealms, ApplicationPure {
    Long id();
    WordName name();
    String description();
    String passTo();
    String virtualPath();
    String topPage();
    boolean writeProtected();
}
