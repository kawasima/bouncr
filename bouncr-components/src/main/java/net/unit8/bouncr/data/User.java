package net.unit8.bouncr.data;

import tools.jackson.databind.annotation.JsonSerialize;
import net.unit8.bouncr.json.UserSerializer;

import java.util.List;

/**
 * User aggregate root in Bouncr.
 *
 * @param id persistent identifier
 * @param account account name used for sign-in
 * @param writeProtected whether mutation is restricted
 * @param groups groups the user belongs to
 * @param userProfileValues profile values assigned to the user
 * @param userLock current lock state
 * @param passwordCredential password credential data
 * @param otpKey one-time-password key
 * @param oidcUsers external OIDC identities linked to this user
 * @param permissions effective permission names
 * @param unverifiedProfiles profile fields pending verification
 */
@JsonSerialize(using = UserSerializer.class)
public record User(
    Long id,
    String account,
    Boolean writeProtected,
    List<Group> groups,
    List<UserProfileValue> userProfileValues,
    UserLock userLock,
    PasswordCredential passwordCredential,
    OtpKey otpKey,
    List<OidcUser> oidcUsers,
    List<String> permissions,
    List<String> unverifiedProfiles
) {
}
