package net.unit8.bouncr.api.encoder;

import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Assignment;
import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.GroupInvitation;
import net.unit8.bouncr.data.Invitation;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.OidcClientMetadata;
import net.unit8.bouncr.data.OidcInvitation;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.bouncr.data.OidcUser;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.Realm;
import net.unit8.bouncr.data.Role;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserAction;
import net.unit8.bouncr.data.UserSession;
import net.unit8.bouncr.data.WebAuthnCredential;
import net.unit8.raoh.encode.Encoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.unit8.raoh.encode.MapEncoders.*;
import static net.unit8.raoh.encode.ObjectEncoders.*;

/**
 * Central registry of raoh {@link Encoder} instances that convert domain objects
 * to {@code Map<String, Object>} for JSON serialization.
 *
 * <p>Each encoder produces a flat map whose keys match the field names declared in
 * the UI's {@code types.ts} (the authoritative naming contract). Public encoders are
 * used directly by resource classes; private {@code _REF} variants encode only the
 * {@code id} and {@code name} fields for use as nested references inside other encoders.
 *
 * <p>Field naming convention is snake_case, except for WebAuthn fields which follow
 * the W3C specification (camelCase).
 */
public final class BouncrJsonEncoders {

    /**
     * Encodes a {@link Permission} to {@code {id, name, description}}.
     */
    public static final Encoder<Permission, Map<String, Object>> PERMISSION = object(
        property("id",          Permission::id,             long_()),
        property("name",        p -> p.name().value(),      string()),
        property("description", Permission::description,    nullable(string()))
    );

    /**
     * Encodes a {@link Role} to {@code {id, name, description}}.
     */
    public static final Encoder<Role, Map<String, Object>> ROLE = object(
        property("id",          Role::id,                   long_()),
        property("name",        r -> r.name().value(),      string()),
        property("description", Role::description,          nullable(string()))
    );

    /**
     * Encodes a {@link Group} to {@code {id, name, description, write_protected}}.
     */
    public static final Encoder<Group, Map<String, Object>> GROUP = object(
        property("id",              Group::id,               long_()),
        property("name",            g -> g.name().value(),   string()),
        property("description",     Group::description,      nullable(string())),
        property("write_protected", Group::writeProtected,   bool())
    );

    /**
     * Encodes a {@link Realm} to {@code {id, name, url, description}}.
     */
    public static final Encoder<Realm, Map<String, Object>> REALM = object(
        property("id",          Realm::id,                  long_()),
        property("name",        r -> r.name().value(),      string()),
        property("url",         Realm::url,                 nullable(string())),
        property("description", Realm::description,         nullable(string()))
    );

    /**
     * Encodes an {@link Application} to
     * {@code {id, name, description, pass_to, virtual_path, top_page}}.
     */
    public static final Encoder<Application, Map<String, Object>> APPLICATION = object(
        property("id",           Application::id,            long_()),
        property("name",         a -> a.name().value(),      string()),
        property("description",  Application::description,   nullable(string())),
        property("pass_to",      Application::passTo,        nullable(string())),
        property("virtual_path", Application::virtualPath,   nullable(string())),
        property("top_page",     Application::topPage,       nullable(string()))
    );

    /**
     * Encodes an {@link OidcProvider} to a flat map containing all client configuration
     * and provider metadata fields. Nested value objects ({@code responseType},
     * {@code tokenEndpointAuthMethod}, URI fields) are unwrapped to their string
     * representations.
     */
    public static final Encoder<OidcProvider, Map<String, Object>> OIDC_PROVIDER = object(
        property("id",                         OidcProvider::id,                                    long_()),
        property("name",                       OidcProvider::name,                                  string()),
        property("client_id",                  p -> p.clientConfig().credentials().clientId(),       string()),
        property("client_secret_set",          p -> p.clientConfig().credentials().clientSecret() != null, bool()),
        property("scope",                      p -> p.clientConfig().scope(),                        nullable(string())),
        property("response_type",              p -> p.clientConfig().responseType(),                 nullable(string().contramap(r -> r.getName()))),
        property("authorization_endpoint",     p -> p.providerMetadata().authorizationEndpoint(),    nullable(string())),
        property("token_endpoint",             p -> p.providerMetadata().tokenEndpoint(),            nullable(string())),
        property("token_endpoint_auth_method", p -> p.clientConfig().tokenEndpointAuthMethod(),      nullable(string().contramap(m -> m.getValue()))),
        property("redirect_uri",               p -> p.clientConfig().redirectUri(),                  nullable(string().contramap(Object::toString))),
        property("jwks_uri",                   p -> p.providerMetadata().jwksUri(),                  nullable(string().contramap(Object::toString))),
        property("issuer",                     p -> p.providerMetadata().issuer(),                   nullable(string())),
        property("pkce_enabled",               p -> p.clientConfig().pkceEnabled(),                  bool())
    );

    /** Minimal reference shape used when embedding a group inside other encoders. */
    private static final Encoder<Group, Map<String, Object>> GROUP_REF = object(
        property("id",   Group::id,             long_()),
        property("name", g -> g.name().value(), string())
    );

    /** Minimal reference shape used when embedding a role inside other encoders. */
    private static final Encoder<Role, Map<String, Object>> ROLE_REF = object(
        property("id",   Role::id,             long_()),
        property("name", r -> r.name().value(), string())
    );

    /** Minimal reference shape used when embedding a realm inside other encoders. */
    private static final Encoder<Realm, Map<String, Object>> REALM_REF = object(
        property("id",   Realm::id,             long_()),
        property("name", r -> r.name().value(), string())
    );

    /**
     * Encodes an {@link Assignment} to
     * {@code {group: {id, name}, role: {id, name}, realm: {id, name}}}.
     */
    public static final Encoder<Assignment, Map<String, Object>> ASSIGNMENT = object(
        property("group", Assignment::group, nested(GROUP_REF)),
        property("role",  Assignment::role,  nested(ROLE_REF)),
        property("realm", Assignment::realm, nested(REALM_REF))
    );

    /**
     * Encodes a {@link UserSession} to
     * {@code {token, remote_address, user_agent, created_at}}.
     * {@code created_at} is formatted via {@link Object#toString()} on the
     * {@link java.time.LocalDateTime} (ISO-8601 local date-time).
     */
    public static final Encoder<UserSession, Map<String, Object>> USER_SESSION = object(
        property("token",          UserSession::token,         string()),
        property("remote_address", UserSession::remoteAddress, nullable(string())),
        property("user_agent",     UserSession::userAgent,     nullable(string())),
        property("created_at",     UserSession::createdAt, nullable(string().contramap(Object::toString)))
    );

    /**
     * Encodes a {@link UserAction} audit log entry to
     * {@code {id, action_type, actor, actor_ip, options, created_at}}.
     * {@code action_type} is the enum constant name; {@code created_at} is ISO-8601 local
     * date-time.
     */
    public static final Encoder<UserAction, Map<String, Object>> USER_ACTION = object(
        property("id",          UserAction::id,          long_()),
        property("action_type", UserAction::actionType,  nullable(string().contramap(Enum::name))),
        property("actor",       UserAction::actor,       nullable(string())),
        property("actor_ip",    UserAction::actorIp,     nullable(string())),
        property("options",     UserAction::options,     nullable(string())),
        property("created_at",  UserAction::createdAt,   nullable(string().contramap(Object::toString)))
    );

    /** Encodes a {@link GroupInvitation} link to {@code {id, group: {id, name}}}. */
    private static final Encoder<GroupInvitation, Map<String, Object>> GROUP_INVITATION = object(
        property("id",    GroupInvitation::id,    long_()),
        property("group", GroupInvitation::group, nested(GROUP_REF))
    );

    /**
     * Encodes an {@link OidcInvitation} link to
     * {@code {id, oidc_provider: {...}, oidc_payload}}.
     */
    private static final Encoder<OidcInvitation, Map<String, Object>> OIDC_INVITATION = object(
        property("id",            OidcInvitation::id,           long_()),
        property("oidc_provider", OidcInvitation::oidcProvider, nested(OIDC_PROVIDER)),
        property("oidc_payload",  OidcInvitation::oidcPayload,  nullable(string()))
    );

    /**
     * Encodes an {@link Invitation} to
     * {@code {id, code, email, invited_at, group_invitations, oidc_invitations}}.
     * Null relation lists are treated as empty rather than omitted, to keep the
     * response shape stable regardless of which relations were eagerly loaded.
     */
    public static final Encoder<Invitation, Map<String, Object>> INVITATION = object(
        property("id",                Invitation::id,    long_()),
        property("code",              Invitation::code,  string()),
        property("email",             Invitation::email, nullable(string())),
        property("invited_at",        Invitation::invitedAt,  nullable(string().contramap(Object::toString))),
        property("group_invitations", Invitation::groupInvitations, list(nested(GROUP_INVITATION))),
        property("oidc_invitations",  Invitation::oidcInvitations, list(nested(OIDC_INVITATION)))
    );

    /**
     * Encodes a sign-in result to {@code {token, account}}.
     * Only includes fields the client needs; never exposes the full {@link User}.
     */
    public static Map<String, Object> encodeSignInSession(UserSession session) {
        return Map.of(
                "token", session.token(),
                "account", session.user().account());
    }

    /**
     * Encodes a {@link WebAuthnCredential} to
     * {@code {id, credential_name, transports, discoverable}}.
     */
    public static final Encoder<WebAuthnCredential, Map<String, Object>> WEBAUTHN_CREDENTIAL = object(
        property("id",              WebAuthnCredential::id,             long_()),
        property("credential_name", WebAuthnCredential::credentialName, withDefault(string(), "")),
        property("transports",      WebAuthnCredential::transports,     withDefault(string(), "")),
        property("discoverable",    WebAuthnCredential::discoverable,   bool())
    );

    /**
     * Encodes an {@link OidcApplication} to
     * {@code {id, name, client_id, home_uri, callback_uri, description,
     * backchannel_logout_uri, frontchannel_logout_uri, permissions, grant_types}}.
     */
    public static Map<String, Object> encodeOidcApplication(OidcApplication app) {
        OidcClientMetadata meta = app.metadata();
        var map = new LinkedHashMap<String, Object>();
        map.put("id", app.id());
        map.put("name", app.name());
        map.put("client_id", app.credentials().clientId());
        map.put("home_uri", meta != null ? meta.homeUriString() : null);
        map.put("callback_uri", meta != null ? meta.callbackUriString() : null);
        map.put("description", app.description());
        map.put("backchannel_logout_uri", meta != null ? meta.backchannelLogoutUriString() : null);
        map.put("frontchannel_logout_uri", meta != null ? meta.frontchannelLogoutUriString() : null);
        map.put("permissions", app.permissions() != null
                ? app.permissions().stream().map(PERMISSION::encode).toList()
                : List.of());
        map.put("grant_types", meta != null && meta.grantTypes() != null
                ? meta.grantTypes().stream().map(GrantType::getValue).toList()
                : GrantType.DEFAULT_GRANT_TYPES);
        return map;
    }

    /**
     * Encodes a newly created {@link OidcApplication} including the plaintext
     * {@code client_secret} (shown once, never stored).
     */
    public static Map<String, Object> encodeOidcApplicationCreated(OidcApplication app, String plaintextSecret) {
        var map = encodeOidcApplication(app);
        map.put("client_secret", plaintextSecret);
        map.remove("permissions");
        return map;
    }

    /**
     * Encodes a sign-up response with profile values flattened to top-level fields.
     */
    public static Map<String, Object> encodeSignUp(User user, Map<String, Object> userProfiles, String password) {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", user.id());
        map.put("account", user.account());
        if (userProfiles != null) {
            userProfiles.forEach((k, v) -> {
                if (v != null) map.put(k, v);
            });
        }
        if (password != null) {
            map.put("password", password);
        }
        return map;
    }

    /**
     * Encodes a {@link User} to a flat map.
     *
     * <p>Profile values are flattened to top-level fields. Optional relations
     * ({@code groups}, {@code permissions}, {@code oidc_providers},
     * {@code unverified_profiles}) are included only when non-null.
     */
    public static Map<String, Object> encodeUser(User user) {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", user.id());
        map.put("account", user.account());

        if (user.userProfileValues() != null) {
            for (var pv : user.userProfileValues()) {
                if (pv.userProfileField() != null && pv.value() != null) {
                    map.put(pv.userProfileField().jsonName(), pv.value());
                }
            }
        }

        if (user.userLock() != null) {
            map.put("lock", Map.of(
                    "lock_level", user.userLock().lockLevel().name(),
                    "locked_at", user.userLock().lockedAt().toString()));
        }

        if (user.groups() != null) {
            map.put("groups", user.groups().stream()
                    .map(GROUP::encode)
                    .toList());
        }

        if (user.permissions() != null) {
            map.put("permissions", user.permissions());
        }

        if (user.oidcUsers() != null) {
            map.put("oidc_providers", user.oidcUsers().stream()
                    .map(OidcUser::providerName)
                    .filter(Objects::nonNull)
                    .toList());
        }

        if (user.unverifiedProfiles() != null && !user.unverifiedProfiles().isEmpty()) {
            map.put("unverified_profiles", user.unverifiedProfiles());
        }

        return map;
    }

    private BouncrJsonEncoders() {}
}
