package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.UserRepository;
import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps OIDC scopes to user profile claims for id_token and UserInfo responses.
 */
public class OidcClaimMapper {

    /**
     * Add user profile claims to the given claims map based on granted scopes.
     */
    public static void addUserClaims(Map<String, Object> claims, long userId, String scope, DSLContext dsl) {
        if (scope == null) return;
        Set<String> scopes = new HashSet<>(Arrays.asList(scope.split("\\s+")));
        UserRepository userRepo = new UserRepository(dsl);

        if (scopes.contains("profile") || scopes.contains("email")) {
            var profileValues = userRepo.loadProfileValues(userId);
            for (var pv : profileValues) {
                String jsonName = pv.userProfileField().jsonName();
                if (scopes.contains("email") && "email".equals(jsonName)) {
                    claims.put("email", pv.value());
                }
                if (scopes.contains("profile")) {
                    if ("name".equals(jsonName) || "family_name".equals(jsonName)
                            || "given_name".equals(jsonName) || "preferred_username".equals(jsonName)) {
                        claims.put(jsonName, pv.value());
                    }
                }
            }
        }
    }

    /**
     * Add user profile claims by account name (for UserInfo endpoint).
     */
    public static void addUserClaimsByAccount(Map<String, Object> claims, String account, String scope, DSLContext dsl) {
        if (scope == null) return;
        Set<String> scopes = new HashSet<>(Arrays.asList(scope.split("\\s+")));
        UserRepository userRepo = new UserRepository(dsl);

        if (scopes.contains("profile") || scopes.contains("email")) {
            userRepo.findByAccount(account).ifPresent(user -> {
                var profileValues = userRepo.loadProfileValues(user.id());
                for (var pv : profileValues) {
                    String jsonName = pv.userProfileField().jsonName();
                    if (scopes.contains("email") && "email".equals(jsonName)) {
                        claims.put("email", pv.value());
                    }
                    if (scopes.contains("profile")) {
                        if ("name".equals(jsonName) || "family_name".equals(jsonName)
                                || "given_name".equals(jsonName) || "preferred_username".equals(jsonName)) {
                            claims.put(jsonName, pv.value());
                        }
                    }
                }
            });
        }
    }
}
