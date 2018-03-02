package net.unit8.bouncr.authz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
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

    @JsonCreator
    public UserPrincipal(@JsonProperty("id") Long id,
                         @JsonProperty("name") String account,
                         @JsonProperty("profiles") Map<String, Object> profiles,
                         @JsonProperty("permissionsByRealm") Map<Long, Set<String>> permissionsByRealm) {
        this.id = id;
        this.account = account;
        this.profiles = profiles;

        this.permissionsByRealm = permissionsByRealm;
    }

    public String getName() {
        return account;
    }

    public Set<String> getPermissions(Long realmId) {
        if (permissionsByRealm == null) {
            return Collections.emptySet();
        }
        return permissionsByRealm.get(realmId);
    }

    public Map<Long, Set<String>> getPermissionsByRealm() {
        if (permissionsByRealm == null) {
            return Collections.emptyMap();
        }
        return permissionsByRealm;
    }

    public Map<String, Object> getProfiles() {
        if (profiles == null) {
            return Collections.emptyMap();
        }
        return profiles;
    }

    public Long getId() {
        return id;
    }
}
