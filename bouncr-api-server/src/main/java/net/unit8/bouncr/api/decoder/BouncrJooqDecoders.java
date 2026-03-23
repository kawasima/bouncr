package net.unit8.bouncr.api.decoder;

import net.unit8.bouncr.data.*;
import net.unit8.raoh.Decoder;
import net.unit8.raoh.Presence;
import net.unit8.raoh.Result;
import net.unit8.raoh.jooq.JooqRecordDecoder;
import org.jooq.Record;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static net.unit8.raoh.Decoders.combine;
import static net.unit8.raoh.Decoders.recover;
import static net.unit8.raoh.ObjectDecoders.*;
import static net.unit8.raoh.jooq.JooqRecordDecoders.*;

/**
 * Centralized jOOQ Record decoders for all Bouncr domain types.
 */
public final class BouncrJooqDecoders {

    private BouncrJooqDecoders() {}

    private static final Decoder<Object, byte[]> BYTES_DECODER = (in, path) -> {
        if (in == null) return Result.fail(path, "required", "is required");
        return Result.ok((byte[]) in);
    };

    // --- Permission ---

    public static final Decoder<Record, Permission> PERMISSION = combine(
            field("permission_id", long_()),
            field("name", string()),
            optionalField("description", string()),
            optionalField("write_protected", bool())
    ).map((id, name, desc, wp) -> new Permission(id, name, desc.orElse(null), wp.orElse(false), null));

    // --- Application ---

    public static final Decoder<Record, Application> APPLICATION = combine(
            field("application_id", long_()),
            field("name", string()),
            optionalField("description", string()),
            optionalField("pass_to", string()),
            optionalField("virtual_path", string()),
            optionalField("top_page", string()),
            optionalField("write_protected", bool())
    ).map((id, name, desc, passTo, virtualPath, topPage, wp) -> new Application(
            id, name, desc.orElse(null), passTo.orElse(null), virtualPath.orElse(null),
            topPage.orElse(null), wp.orElse(false), null));

    // --- Realm ---

    public static final Decoder<Record, Realm> REALM = combine(
            field("realm_id", long_()),
            field("name", string()),
            optionalField("name_lower", string()),
            optionalField("url", string()),
            optionalField("description", string()),
            optionalField("write_protected", bool())
    ).map((id, name, nameLower, url, desc, wp) -> new Realm(
            id, name, nameLower.orElse(null), url.orElse(null), desc.orElse(null),
            null, wp.orElse(false), null));

    // --- Role ---

    public static final Decoder<Record, Role> ROLE = combine(
            field("role_id", long_()),
            field("name", string()),
            optionalField("description", string()),
            optionalField("write_protected", bool())
    ).map((id, name, desc, wp) -> new Role(id, name, desc.orElse(null), wp.orElse(false), null));

    // --- Group ---

    public static final Decoder<Record, Group> GROUP = combine(
            field("group_id", long_()),
            field("name", string()),
            optionalField("description", string()),
            optionalField("write_protected", bool())
    ).map((id, name, desc, wp) -> new Group(id, name, desc.orElse(null), wp.orElse(false), null));

    // --- User ---

    public static final Decoder<Record, User> USER = combine(
            field("user_id", long_()),
            field("account", string()),
            optionalField("write_protected", bool())
    ).map((id, account, wp) -> new User(id, account, wp.orElse(false),
            null, null, null, null, null, null));

    // --- PasswordCredential ---

    public static final Decoder<Record, PasswordCredential> PASSWORD_CREDENTIAL = combine(
            optionalField("password", BYTES_DECODER),
            optionalField("salt", string()),
            field("initial", bool()),
            field("created_at", dateTime())
    ).map((password, salt, initial, createdAt) -> new PasswordCredential(
            null, password.orElse(null), salt.orElse(null), initial, createdAt));

    // --- OtpKey ---

    public static final Decoder<Record, OtpKey> OTP_KEY =
            field("otp_key", BYTES_DECODER)
                    .map(key -> new OtpKey(key));

    // --- UserLock ---

    public static final Decoder<Record, UserLock> USER_LOCK = combine(
            field("lock_level", string()),
            field("locked_at", dateTime())
    ).map((level, lockedAt) -> new UserLock(LockLevel.valueOf(level), lockedAt));

    // --- Nullable wrappers for LEFT JOIN ---

    private static final JooqRecordDecoder<PasswordCredential> NULLABLE_PASSWORD_CREDENTIAL =
            recover(nested(PASSWORD_CREDENTIAL::decode), (PasswordCredential) null)::decode;

    private static final JooqRecordDecoder<OtpKey> NULLABLE_OTP_KEY =
            recover(nested(OTP_KEY::decode), (OtpKey) null)::decode;

    private static final JooqRecordDecoder<UserLock> NULLABLE_USER_LOCK =
            recover(nested(USER_LOCK::decode), (UserLock) null)::decode;

    // --- UserCredentials (for sign-in) ---

    public static final Decoder<Record, UserCredentials> USER_CREDENTIALS = combine(
            field("user_id", long_()),
            field("account", string()),
            nested(NULLABLE_PASSWORD_CREDENTIAL),
            nested(NULLABLE_OTP_KEY),
            nested(NULLABLE_USER_LOCK)
    ).map(UserCredentials::new);

    // --- UserSession ---

    public static final Decoder<Record, UserSession> USER_SESSION = combine(
            field("user_session_id", long_()),
            field("token", string()),
            optionalField("remote_address", string()),
            optionalField("user_agent", string()),
            field("created_at", dateTime())
    ).map((sessionId, token, remoteAddr, userAgent, createdAt) -> new UserSession(
            sessionId, null, token, remoteAddr.orElse(null), userAgent.orElse(null), createdAt));

    public static final Decoder<Record, UserSession> USER_SESSION_WITH_USER = combine(
            field("user_session_id", long_()),
            field("user_id", long_()),
            field("account", string()),
            optionalField("write_protected", bool()),
            field("token", string()),
            optionalField("remote_address", string()),
            optionalField("user_agent", string()),
            field("created_at", dateTime())
    ).map((sessionId, userId, account, wp, token, remoteAddr, userAgent, createdAt) -> new UserSession(
            sessionId,
            new User(userId, account, wp.orElse(false),
                    null, null, null, null, null, null),
            token,
            remoteAddr.orElse(null),
            userAgent.orElse(null),
            createdAt));

    // --- UserAction ---

    public static final Decoder<Record, UserAction> USER_ACTION = combine(
            field("user_action_id", long_()),
            field("action_id", long_()),
            field("actor", string()),
            optionalNullableField("actor_ip", string()),
            optionalNullableField("options", string()),
            field("created_at", dateTime())
    ).map((id, actionId, actor, actorIp, options, createdAt) -> new UserAction(
            id, ActionType.of(actionId), actor,
            actorIp instanceof Presence.Present<String> p ? p.value() : null,
            options instanceof Presence.Present<String> p ? p.value() : null,
            createdAt));

    // --- Invitation ---

    public static final Decoder<Record, Invitation> INVITATION = combine(
            field("invitation_id", long_()),
            field("code", string()),
            field("email", string()),
            field("invited_at", dateTime())
    ).map((id, code, email, invitedAt) -> new Invitation(id, code, email, invitedAt, null, null));

    // --- GroupInvitation ---

    public static final Decoder<Record, GroupInvitation> GROUP_INVITATION = combine(
            field("group_invitation_id", long_()),
            field("group_id", long_()),
            field("name", string()),
            optionalField("description", string()),
            optionalField("write_protected", bool())
    ).map((giId, groupId, name, desc, wp) -> new GroupInvitation(
            giId, null,
            new Group(groupId, name, desc.orElse(null), wp.orElse(false), null)));

    // --- OidcInvitation ---

    public static final Decoder<Record, OidcInvitation> OIDC_INVITATION = combine(
            field("oidc_invitation_id", long_()),
            optionalField("oidc_payload", string())
    ).map((id, payload) -> new OidcInvitation(id, null, null, payload.orElse(null)));

    // --- Assignment ---

    public static final Decoder<Record, Assignment> ASSIGNMENT = combine(
            field("group_id", long_()),
            field("group_name", string()),
            optionalField("group_description", string()),
            optionalField("group_write_protected", bool()),
            field("role_id", long_()),
            field("role_name", string()),
            optionalField("role_description", string()),
            optionalField("role_write_protected", bool())
    ).map((groupId, groupName, groupDesc, groupWp, roleId, roleName, roleDesc, roleWp) -> new Assignment(
            new Group(groupId, groupName, groupDesc.orElse(null), groupWp.orElse(false), null),
            new Role(roleId, roleName, roleDesc.orElse(null), roleWp.orElse(false), null),
            null));

    // --- PasswordResetChallenge ---

    public static final Decoder<Record, PasswordResetChallenge> PASSWORD_RESET_CHALLENGE = combine(
            field("id", long_()),
            field("code", string()),
            field("expires_at", dateTime())
    ).map((id, code, expiresAt) -> new PasswordResetChallenge(id, null, code, expiresAt));

    public static final Decoder<Record, PasswordResetChallenge> PRC_WITH_USER = combine(
            field("id", long_()),
            field("user_id", long_()),
            field("account", string()),
            optionalField("write_protected", bool()),
            field("code", string()),
            field("expires_at", dateTime())
    ).map((id, userId, account, wp, code, expiresAt) -> new PasswordResetChallenge(
            id,
            new User(userId, account, wp.orElse(false),
                    null, null, null, null, null, null),
            code, expiresAt));

    // --- UserProfileField ---

    public static final Decoder<Record, UserProfileField> USER_PROFILE_FIELD = combine(
            field("user_profile_field_id", long_()),
            field("name", string()),
            optionalField("json_name", string()),
            field("is_required", bool()),
            field("is_identity", bool()),
            optionalField("regular_expression", string()),
            optionalField("max_length", int_()),
            optionalField("min_length", int_()),
            field("needs_verification", bool()),
            optionalField("position", int_())
    ).map((id, name, jsonName, isRequired, isIdentity, regex, maxLen, minLen, needsVerification, position) ->
            new UserProfileField(id, name, jsonName.orElse(null), isRequired, isIdentity,
                    regex.orElse(null), maxLen.orElse(null), minLen.orElse(null),
                    needsVerification, position.orElse(null)));

    // --- OidcProvider ---

    public static final Decoder<Record, OidcProvider> OIDC_PROVIDER = combine(
            field("oidc_provider_id", long_()),
            field("name", string()),
            optionalField("name_lower", string()),
            optionalField("client_id", string()),
            optionalField("client_secret", string()),
            optionalField("scope", string()),
            optionalField("response_type", string()),
            optionalField("token_endpoint", string()),
            optionalField("authorization_endpoint", string()),
            optionalField("token_endpoint_auth_method", string()),
            optionalField("redirect_uri", string()),
            optionalField("jwks_uri", string()),
            optionalField("issuer", string()),
            optionalField("pkce_enabled", bool())
    ).map((id, name, nameLower, clientId, clientSecret, scope, responseType,
           tokenEndpoint, authorizationEndpoint, authMethod, redirectUri, jwksUri, issuer, pkceEnabled) -> {
        return new OidcProvider(
                id, name, nameLower.orElse(null),
                clientId.orElse(null), clientSecret.orElse(null),
                scope.orElse(null),
                responseType.map(ResponseType::of).orElse(null),
                tokenEndpoint.orElse(null),
                authorizationEndpoint.orElse(null),
                authMethod.map(TokenEndpointAuthMethod::of).orElse(null),
                redirectUri.map(URI::create).orElse(null),
                jwksUri.map(BouncrJooqDecoders::toUrl).orElse(null),
                issuer.orElse(null),
                pkceEnabled.orElse(false));
    });

    // --- OidcApplication ---

    public static final Decoder<Record, OidcApplication> OIDC_APPLICATION = combine(
            field("oidc_application_id", long_()),
            field("name", string()),
            optionalField("name_lower", string()),
            optionalField("client_id", string()),
            optionalField("client_secret", string()),
            optionalField("private_key", BYTES_DECODER),
            optionalField("public_key", BYTES_DECODER),
            optionalField("home_url", string()),
            optionalField("callback_url", string()),
            optionalField("description", string()),
            optionalField("backchannel_logout_uri", string()),
            optionalField("frontchannel_logout_uri", string())
    ).map((id, name, nameLower, clientId, clientSecret, privateKey, publicKey,
            homeUrl, callbackUrl, desc, backchannelLogoutUri, frontchannelLogoutUri) -> {
        return new OidcApplication(
                id, name, nameLower.orElse(null),
                clientId.orElse(null), clientSecret.orElse(null),
                privateKey.orElse(null), publicKey.orElse(null),
                homeUrl.map(BouncrJooqDecoders::toUrl).orElse(null),
                callbackUrl.map(BouncrJooqDecoders::toUrl).orElse(null),
                desc.orElse(null),
                backchannelLogoutUri.map(BouncrJooqDecoders::toUrl).orElse(null),
                frontchannelLogoutUri.map(BouncrJooqDecoders::toUrl).orElse(null),
                null);
    });

    private static URL toUrl(String raw) {
        try {
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + raw, e);
        }
    }
}
