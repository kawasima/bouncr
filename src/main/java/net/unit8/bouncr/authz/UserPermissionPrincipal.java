package net.unit8.bouncr.authz;

import enkan.security.UserPrincipal;

import java.io.Serializable;
import java.util.Set;

/**
 * A user principal with permissions.
 *
 * @author kawasima
 */
public class UserPermissionPrincipal implements UserPrincipal, Serializable {
    private final Long id;
    private final String email;
    private final String account;
    private final Set<String> permissions;

    public UserPermissionPrincipal(Long id, String account, String email, Set<String> permissions) {
        this.id = id;
        this.account = account;
        this.email = email;
        this.permissions = permissions;
    }

    @Override
    public String getName() {
        return account;
    }


    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }
}
