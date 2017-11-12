package net.unit8.bouncr.authz;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * A user principal with permissions.
 *
 * @author miyamoen
 */
public class UserPrincipal implements Serializable {
    private final Long id;
    private final Map<String, Object> profiles;

    private final String account;
    private final Map<Long, Set<String>> permissionsByRealm;

    public UserPrincipal(Long id, String account, Map<String, Object> profiles, Map<Long, Set<String>> permissionsByRealm) {
        this.id = id;
        this.account = account;
        this.profiles = profiles;

        this.permissionsByRealm = permissionsByRealm;
    }

    public String getName() {
        return account;
    }

    public Set<String> getPermissions(Long realmId) {
        return permissionsByRealm.get(realmId);
    }

    public Map<String, Object> getProfiles() {
        return profiles;
    }

    public Long getId() {
        return id;
    }
}
