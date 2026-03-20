package net.unit8.bouncr.data;

import tools.jackson.databind.annotation.JsonSerialize;
import net.unit8.bouncr.json.UserSerializer;

import java.util.List;

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
