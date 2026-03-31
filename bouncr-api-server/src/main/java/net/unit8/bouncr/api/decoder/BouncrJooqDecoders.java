package net.unit8.bouncr.api.decoder;

import net.unit8.bouncr.data.*;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.Result;
import net.unit8.raoh.jooq.JooqRecordDecoder;
import org.jooq.Record;

import java.net.URI;

import static net.unit8.raoh.decode.Decoders.*;
import static net.unit8.raoh.decode.ObjectDecoders.*;
import static net.unit8.raoh.jooq.JooqRecordDecoders.*;

/**
 * Centralized jOOQ Record decoders for all Bouncr domain types.
 */
public final class BouncrJooqDecoders {

    private BouncrJooqDecoders() {}

    // --- Primitive helpers ---

    private static Decoder<Object, byte[]> bytes() {
        return (in, path) -> {
            if (in == null) return Result.fail(path, "required", "is required");
            return Result.ok((byte[]) in);
        };
    }

    /** Decodes a non-blank string to URI, treating null/blank as null. */
    private static Decoder<Object, URI> nullableUri() {
        return (in, path) -> {
            if (in == null) return Result.ok(null);
            if (in instanceof String s && !s.isBlank()) {
                try {
                    return Result.ok(URI.create(s.trim()));
                } catch (IllegalArgumentException e) {
                    return Result.fail(path, "invalid_format", "not a valid URI");
                }
            }
            return Result.ok(null);
        };
    }

    private static <T> JooqRecordDecoder<T> nullable(Decoder<Record, T> dec) {
        return recover(nested(dec::decode), (T) null)::decode;
    }

    // --- Permission ---

    public static final Decoder<Record, Permission> PERMISSION = combine(
            field("permission_id", long_()),
            nested(
                    combine(
                            field("name", string()).map(PermissionName::new),
                            withDefault(field("description", string()), (String) null)
                    ).map(PermissionSpec::new)::decode
            ),
            withDefault(field("write_protected", bool()), false)
    ).map(Permission::new);

    // --- Application ---
    public static final Decoder<Record, Application> APPLICATION = combine(
            field("application_id", long_()),
            nested(
                    combine(
                            field("name", string()).map(WordName::new),
                            withDefault(field("description", string()), (String) null),
                            withDefault(field("pass_to", string()), (String) null),
                            withDefault(field("virtual_path", string()), (String) null),
                            withDefault(field("top_page", string()), (String) null)
                    ).map(ApplicationSpec::new)::decode
            ),
            withDefault(field("write_protected", bool()), false)
    ).map(ApplicationPure::new);

    // --- Realm ---

    public static final Decoder<Record, Realm> REALM = combine(
            field("realm_id", long_()),
            nested(
                    combine(
                            field("name", string()).map(WordName::new),
                            withDefault(field("url", string()), (String) null),
                            withDefault(field("description", string()), (String) null)
                    ).map(RealmSpec::new)::decode
            ),
            withDefault(field("write_protected", bool()), false)
    ).map(RealmPure::new);

    // --- Role ---

    public static final Decoder<Record, Role> ROLE = combine(
            field("role_id", long_()),
            nested(
                    combine(
                            field("name", string()).map(WordName::new),
                            withDefault(field("description", string()), (String) null)
                    ).map(RoleSpec::new)::decode
            ),
            withDefault(field("write_protected", bool()), false)
    ).map(RolePure::new);

    // --- Group ---

    public static final Decoder<Record, Group> GROUP = combine(
            field("group_id", long_()),
            nested(
                    combine(
                            field("name", string()).map(WordName::new),
                            withDefault(field("description", string()), (String) null)
                    ).map(GroupSpec::new)::decode
            ),
            withDefault(field("write_protected", bool()), false)
    ).map(GroupPure::new);

    // --- User ---

    public static final Decoder<Record, User> USER = combine(
            field("user_id", long_()),
            field("account", string()),
            withDefault(field("write_protected", bool()), false)
    ).map(User::of);

    // --- PasswordCredential ---

    public static final Decoder<Record, PasswordCredential> PASSWORD_CREDENTIAL = combine(
            withDefault(field("password", bytes()), (byte[]) null),
            withDefault(field("salt", string()), (String) null),
            field("initial", bool()),
            field("created_at", dateTime())
    ).map(PasswordCredential::of);

    // --- OtpKey ---

    public static final Decoder<Record, OtpKey> OTP_KEY =
            field("otp_key", bytes()).map(OtpKey::new);

    // --- UserLock ---

    public static final Decoder<Record, UserLock> USER_LOCK = combine(
            field("lock_level", enumOf(LockLevel.class)),
            field("locked_at", dateTime())
    ).map(UserLock::new);

    // --- Nullable wrappers for LEFT JOIN ---

    private static final JooqRecordDecoder<PasswordCredential> NULLABLE_PASSWORD_CREDENTIAL = nullable(PASSWORD_CREDENTIAL);
    private static final JooqRecordDecoder<OtpKey> NULLABLE_OTP_KEY = nullable(OTP_KEY);
    private static final JooqRecordDecoder<UserLock> NULLABLE_USER_LOCK = nullable(USER_LOCK);

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
            withDefault(field("remote_address", string()), (String) null),
            withDefault(field("user_agent", string()), (String) null),
            field("created_at", dateTime())
    ).map((sessionId, token, remoteAddr, userAgent, createdAt) -> new UserSession(
            sessionId, null, token, remoteAddr, userAgent, createdAt));

    public static final Decoder<Record, UserSession> USER_SESSION_WITH_USER = combine(
            field("user_session_id", long_()),
            nested(USER::decode),
            field("token", string()),
            withDefault(field("remote_address", string()), (String) null),
            withDefault(field("user_agent", string()), (String) null),
            field("created_at", dateTime())
    ).map(UserSession::new);

    // --- UserAction ---

    public static final Decoder<Record, UserAction> USER_ACTION = combine(
            field("user_action_id", long_()),
            field("action_id", long_()),
            field("actor", string()),
            withDefault(field("actor_ip", string()), (String) null),
            withDefault(field("options", string()), (String) null),
            field("created_at", dateTime())
    ).map((id, actionId, actor, actorIp, options, createdAt) ->
            new UserAction(id, ActionType.of(actionId), actor, actorIp, options, createdAt));

    // --- Invitation ---

    public static final Decoder<Record, Invitation> INVITATION = combine(
            field("invitation_id", long_()),
            field("code", string()),
            field("email", string()),
            field("invited_at", dateTime())
    ).map(Invitation::of);

    // --- GroupInvitation ---

    public static final Decoder<Record, GroupInvitation> GROUP_INVITATION = combine(
            field("group_invitation_id", long_()),
            field("group_id", long_()),
            field("name", string()),
            withDefault(field("description", string()), (String) null),
            withDefault(field("write_protected", bool()), false)
    ).map((giId, groupId, name, desc, wp) ->
            new GroupInvitation(giId, null, new GroupPure(groupId, new GroupSpec(new WordName(name), desc), wp)));

    // --- OidcInvitation ---

    public static final Decoder<Record, OidcInvitation> OIDC_INVITATION = combine(
            field("oidc_invitation_id", long_()),
            withDefault(field("oidc_payload", string()), (String) null)
    ).map((id, payload) -> new OidcInvitation(id, null, null, payload));

    // --- Assignment (flat JOIN structuring with nested) ---

    private static final JooqRecordDecoder<Group> GROUP_FROM_ASSIGNMENT = combine(
            field("group_id", long_()),
            nested(
                    combine(
                            field("group_name", string()).map(WordName::new),
                            withDefault(field("group_description", string()), (String) null)
                    ).map(GroupSpec::new)::decode
            ),
            withDefault(field("group_write_protected", bool()), false)
    ).<Group>map((id, spec, wp) -> new GroupPure(id, spec, wp))::decode;

    private static final JooqRecordDecoder<Role> ROLE_FROM_ASSIGNMENT = combine(
            field("role_id", long_()),
            nested(
                    combine(
                            field("role_name", string()).map(WordName::new),
                            withDefault(field("role_description", string()), (String) null)
                    ).map(RoleSpec::new)::decode
            ),
            withDefault(field("role_write_protected", bool()), false)
    ).<Role>map((id, spec, wp) -> new RolePure(id, spec, wp))::decode;

    private static final JooqRecordDecoder<Realm> REALM_FROM_ASSIGNMENT = combine(
            field("realm_id", long_()),
            nested(
                    combine(
                            field("realm_name", string()).map(WordName::new),
                            withDefault(field("realm_url", string()), (String) null),
                            withDefault(field("realm_description", string()), (String) null)
                    ).map(RealmSpec::new)::decode
            ),
            withDefault(field("realm_write_protected", bool()), false)
    ).<Realm>map((id, spec, wp) -> new RealmPure(id, spec, wp))::decode;

    public static final Decoder<Record, Assignment> ASSIGNMENT = combine(
            nested(GROUP_FROM_ASSIGNMENT),
            nested(ROLE_FROM_ASSIGNMENT)
    ).map((group, role) -> new Assignment(group, role, null));

    public static final Decoder<Record, Assignment> ASSIGNMENT_WITH_REALM = combine(
            nested(GROUP_FROM_ASSIGNMENT),
            nested(ROLE_FROM_ASSIGNMENT),
            nested(REALM_FROM_ASSIGNMENT)
    ).map(Assignment::new);

    // --- PasswordResetChallenge ---

    public static final Decoder<Record, PasswordResetChallenge> PASSWORD_RESET_CHALLENGE = combine(
            field("id", long_()),
            field("code", string()),
            field("expires_at", dateTime())
    ).map(PasswordResetChallenge::of);

    public static final Decoder<Record, PasswordResetChallenge> PRC_WITH_USER = combine(
            field("id", long_()),
            nested(USER::decode),
            field("code", string()),
            field("expires_at", dateTime())
    ).map(PasswordResetChallenge::new);

    // --- UserProfileField ---

    public static final Decoder<Record, UserProfileField> USER_PROFILE_FIELD = combine(
            field("user_profile_field_id", long_()),
            field("name", string()),
            withDefault(field("json_name", string()), (String) null),
            field("is_required", bool()),
            field("is_identity", bool()),
            withDefault(field("regular_expression", string()), (String) null),
            withDefault(field("max_length", int_()), (Integer) null),
            withDefault(field("min_length", int_()), (Integer) null),
            field("needs_verification", bool()),
            withDefault(field("position", int_()), (Integer) null)
    ).map(UserProfileField::new);

    // --- OidcProvider ---

    public static final Decoder<Record, OidcProvider> OIDC_PROVIDER = combine(
            field("oidc_provider_id", long_()),
            field("name", string()),
            withDefault(field("name_lower", string()), (String) null),
            withDefault(field("client_id", string()), (String) null),
            withDefault(field("client_secret", string()), (String) null),
            withDefault(field("scope", string()), (String) null),
            optionalField("response_type", string()),
            withDefault(field("token_endpoint", string()), (String) null),
            withDefault(field("authorization_endpoint", string()), (String) null),
            optionalField("token_endpoint_auth_method", string()),
            field("redirect_uri", nullableUri()),
            field("jwks_uri", nullableUri()),
            withDefault(field("issuer", string()), (String) null),
            withDefault(field("pkce_enabled", bool()), false)
    ).map((id, name, nameLower, clientId, clientSecret, scope, responseType,
           tokenEndpoint, authorizationEndpoint, authMethod, redirectUri, jwksUri, issuer, pkceEnabled) ->
            new OidcProvider(id, name, nameLower,
                    new OidcProviderMetadata(authorizationEndpoint, tokenEndpoint, jwksUri, issuer),
                    new OidcProviderClientConfig(
                            new ClientCredentials(clientId, clientSecret),
                            scope,
                            responseType.map(ResponseType::of).orElse(ResponseType.CODE),
                            authMethod.map(TokenEndpointAuthMethod::of).orElse(TokenEndpointAuthMethod.CLIENT_SECRET_BASIC),
                            redirectUri, pkceEnabled)));

    // --- OidcApplication ---

    public static final Decoder<Record, OidcApplication> OIDC_APPLICATION = combine(
            field("oidc_application_id", long_()),
            field("name", string()),
            withDefault(field("name_lower", string()), (String) null),
            withDefault(field("client_id", string()), (String) null),
            withDefault(field("client_secret", string()), (String) null),
            withDefault(field("private_key", bytes()), (byte[]) null),
            withDefault(field("public_key", bytes()), (byte[]) null),
            field("home_uri", nullableUri()),
            field("callback_uri", nullableUri()),
            withDefault(field("description", string()), (String) null),
            field("backchannel_logout_uri", nullableUri()),
            field("frontchannel_logout_uri", nullableUri())
    ).map((id, name, nameLower, clientId, clientSecret, privateKey, publicKey,
            homeUri, callbackUri, desc, backchannelLogoutUri, frontchannelLogoutUri) ->
            new OidcApplication(id, name, nameLower,
                    new ClientCredentials(clientId, clientSecret),
                    (privateKey != null || publicKey != null) ? new SigningKeyPair(privateKey, publicKey) : null,
                    new OidcClientMetadata(homeUri, callbackUri, backchannelLogoutUri, frontchannelLogoutUri, null),
                    desc, null));

}
