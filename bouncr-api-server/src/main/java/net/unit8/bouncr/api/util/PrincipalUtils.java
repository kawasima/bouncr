package net.unit8.bouncr.api.util;

import enkan.security.bouncr.UserPermissionPrincipal;

public final class PrincipalUtils {
    private PrincipalUtils() {}

    public static boolean isClientToken(UserPermissionPrincipal principal) {
        return principal != null
            && "client".equals(principal.getProfiles().get("token_type"));
    }
}
