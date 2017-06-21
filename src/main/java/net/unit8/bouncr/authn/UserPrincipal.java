package net.unit8.bouncr.authn;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author kawasima
 */
public class UserPrincipal implements Principal, Serializable {
    private final String name;

    public UserPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
