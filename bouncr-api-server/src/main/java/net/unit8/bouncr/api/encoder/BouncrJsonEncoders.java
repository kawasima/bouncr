package net.unit8.bouncr.api.encoder;

import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Assignment;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.GroupInvitation;
import net.unit8.bouncr.data.Invitation;
import net.unit8.bouncr.data.OidcInvitation;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.Realm;
import net.unit8.bouncr.data.Role;
import net.unit8.bouncr.data.UserAction;
import net.unit8.bouncr.data.UserSession;
import net.unit8.raoh.encode.Encoder;

import java.util.Map;

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
 * <p>Field naming conventions follow the UI contract per entity type and are intentionally
 * mixed (some snake_case, some camelCase) until a separate issue unifies them.
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
     * Encodes a {@link Group} to {@code {id, name, description, writeProtected}}.
     */
    public static final Encoder<Group, Map<String, Object>> GROUP = object(
        property("id",             Group::id,               long_()),
        property("name",           g -> g.name().value(),   string()),
        property("description",    Group::description,      nullable(string())),
        property("writeProtected", Group::writeProtected,   bool())
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
        property("id",                      OidcProvider::id,                                                                                            long_()),
        property("name",                    OidcProvider::name,                                                                                          string()),
        property("clientId",        p -> p.clientConfig().credentials().clientId(),                          string()),
        property("clientSecretSet", p -> p.clientConfig().credentials().clientSecret() != null,             bool()),
        property("scope",                   p -> p.clientConfig().scope(),                                                                               nullable(string())),
        property("responseType",            p -> p.clientConfig().responseType(),              nullable(string().contramap(r -> r.getName()))),
        property("authorizationEndpoint",   p -> p.providerMetadata().authorizationEndpoint(), nullable(string())),
        property("tokenEndpoint",           p -> p.providerMetadata().tokenEndpoint(),         nullable(string())),
        property("tokenEndpointAuthMethod", p -> p.clientConfig().tokenEndpointAuthMethod(),   nullable(string().contramap(m -> m.getValue()))),
        property("redirectUri",             p -> p.clientConfig().redirectUri(),                nullable(string().contramap(Object::toString))),
        property("jwksUri",                 p -> p.providerMetadata().jwksUri(),                nullable(string().contramap(Object::toString))),
        property("issuer",                  p -> p.providerMetadata().issuer(),                                                                          nullable(string())),
        property("pkceEnabled",             p -> p.clientConfig().pkceEnabled(),                                                                         bool())
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
     * {@code {id, oidcProvider: {...}, oidcPayload}}.
     */
    private static final Encoder<OidcInvitation, Map<String, Object>> OIDC_INVITATION = object(
        property("id",           OidcInvitation::id,           long_()),
        property("oidcProvider", OidcInvitation::oidcProvider, nested(OIDC_PROVIDER)),
        property("oidcPayload",  OidcInvitation::oidcPayload,  nullable(string()))
    );

    /**
     * Encodes an {@link Invitation} to
     * {@code {id, code, email, invitedAt, groupInvitations, oidcInvitations}}.
     * Null relation lists are treated as empty rather than omitted, to keep the
     * response shape stable regardless of which relations were eagerly loaded.
     */
    public static final Encoder<Invitation, Map<String, Object>> INVITATION = object(
        property("id",               Invitation::id,    long_()),
        property("code",             Invitation::code,  string()),
        property("email",            Invitation::email, nullable(string())),
        property("invitedAt",        Invitation::invitedAt,  nullable(string().contramap(Object::toString))),
        property("groupInvitations", Invitation::groupInvitations, list(nested(GROUP_INVITATION))),
        property("oidcInvitations",  Invitation::oidcInvitations, list(nested(OIDC_INVITATION)))
    );

    private BouncrJsonEncoders() {}
}
