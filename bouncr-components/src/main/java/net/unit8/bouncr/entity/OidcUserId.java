package net.unit8.bouncr.entity;

import java.io.Serializable;
import java.util.Objects;

public class OidcUserId implements Serializable {
    private Long oidcProvider;
    private Long user;

    public OidcUserId() {}

    public OidcUserId(Long oidcProvider, Long user) {
        this.oidcProvider = oidcProvider;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OidcUserId)) return false;
        OidcUserId that = (OidcUserId) o;
        return Objects.equals(oidcProvider, that.oidcProvider) &&
               Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oidcProvider, user);
    }
}
