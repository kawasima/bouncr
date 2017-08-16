package net.unit8.bouncr.authz;

import enkan.security.UserPrincipal;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * A user principal with permissions.
 *
 * @author kawasima
 */
public class UserPermissionPrincipal implements UserPrincipal, Serializable {
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


    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
