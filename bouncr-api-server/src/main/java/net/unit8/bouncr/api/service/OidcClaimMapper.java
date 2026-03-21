package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.UserProfileValue;
import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps OIDC scopes to user profile claims for id_token and UserInfo responses.
 */
public class OidcClaimMapper {

    private static final Set<String> PROFILE_CLAIMS = Set.of(
            "name", "family_name", "given_name", "preferred_username");

    /**
     * Add user profile claims by userId.
     */
    public static void addUserClaims(Map<String, Object> claims, long userId, String scope, DSLContext dsl) {
        if (scope == null) return;
        Set<String> scopes = parseScopes(scope);
        UserRepository userRepo = new UserRepository(dsl);
        mapProfileValues(claims, scopes, userRepo.loadProfileValues(userId));
    }

    /**
     * Add user profile claims by account name (for UserInfo endpoint).
     */
    public static void addUserClaimsByAccount(Map<String, Object> claims, String account, String scope, DSLContext dsl) {
        if (scope == null) return;
        Set<String> scopes = parseScopes(scope);
        UserRepository userRepo = new UserRepository(dsl);
        userRepo.findByAccount(account).ifPresent(user ->
                mapProfileValues(claims, scopes, userRepo.loadProfileValues(user.id())));
    }

    private static Set<String> parseScopes(String scope) {
        return new HashSet<>(Arrays.asList(scope.split("\\s+")));
    }

    private static void mapProfileValues(Map<String, Object> claims, Set<String> scopes,
                                         List<UserProfileValue> profileValues) {
        if (!scopes.contains("profile") && !scopes.contains("email")) return;
        for (var pv : profileValues) {
            String jsonName = pv.userProfileField().jsonName();
            if (scopes.contains("email") && "email".equals(jsonName)) {
                claims.put("email", pv.value());
            }
            if (scopes.contains("profile") && PROFILE_CLAIMS.contains(jsonName)) {
                claims.put(jsonName, pv.value());
            }
        }
    }
}
