package net.unit8.bouncr.authz;

import java.security.Principal;
import java.util.Set;

/**
 * @author kawasima
 */
public class UserPermissionPrincipal implements Principal {
    private final String name;
    private final Set<String> permissions;

    public UserPermissionPrincipal(String name, Set<String> permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isAllowed(String permission) {
        return permissions.contains(permission);
    }
}
