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
import net.unit8.raoh.encoder.Encoder;

import java.util.List;
import java.util.Map;

import static net.unit8.raoh.encoder.MapEncoders.*;
import static net.unit8.raoh.encoder.ObjectEncoders.*;

public final class BouncrJsonEncoders {

    public static final Encoder<Permission, Map<String, Object>> PERMISSION = object(
        property("id",          Permission::id,             long_()),
        property("name",        p -> p.name().value(),      string()),
        property("description", Permission::description,    nullable(string()))
    );

    public static final Encoder<Role, Map<String, Object>> ROLE = object(
        property("id",          Role::id,                   long_()),
        property("name",        r -> r.name().value(),      string()),
        property("description", Role::description,          nullable(string()))
    );

    public static final Encoder<Group, Map<String, Object>> GROUP = object(
        property("id",             Group::id,               long_()),
        property("name",           g -> g.name().value(),   string()),
        property("description",    Group::description,      nullable(string())),
        property("writeProtected", Group::writeProtected,   bool())
    );

    public static final Encoder<Realm, Map<String, Object>> REALM = object(
        property("id",          Realm::id,                  long_()),
        property("name",        r -> r.name().value(),      string()),
        property("url",         Realm::url,                 nullable(string())),
        property("description", Realm::description,         nullable(string()))
    );

    public static final Encoder<Application, Map<String, Object>> APPLICATION = object(
        property("id",           Application::id,            long_()),
        property("name",         a -> a.name().value(),      string()),
        property("description",  Application::description,   nullable(string())),
        property("pass_to",      Application::passTo,        nullable(string())),
        property("virtual_path", Application::virtualPath,   nullable(string())),
        property("top_page",     Application::topPage,       nullable(string()))
    );

    public static final Encoder<OidcProvider, Map<String, Object>> OIDC_PROVIDER = object(
        property("id",                      OidcProvider::id,                                                      long_()),
        property("name",                    OidcProvider::name,                                                    string()),
        property("clientId",                p -> p.clientConfig().credentials().clientId(),                        string()),
        property("clientSecret",            p -> p.clientConfig().credentials().clientSecret(),                    nullable(string())),
        property("scope",                   p -> p.clientConfig().scope(),                                         nullable(string())),
        property("responseType",            p -> p.clientConfig().responseType() != null ? p.clientConfig().responseType().getName() : null, nullable(string())),
        property("authorizationEndpoint",   p -> p.providerMetadata().authorizationEndpoint(),                     nullable(string())),
        property("tokenEndpoint",           p -> p.providerMetadata().tokenEndpoint(),                             nullable(string())),
        property("tokenEndpointAuthMethod", p -> p.clientConfig().tokenEndpointAuthMethod() != null ? p.clientConfig().tokenEndpointAuthMethod().getValue() : null, nullable(string())),
        property("redirectUri",             p -> p.clientConfig().redirectUri() != null ? p.clientConfig().redirectUri().toString() : null, nullable(string())),
        property("jwksUri",                 p -> p.providerMetadata().jwksUri() != null ? p.providerMetadata().jwksUri().toString() : null, nullable(string())),
        property("issuer",                  p -> p.providerMetadata().issuer(),                                    nullable(string())),
        property("pkceEnabled",             p -> p.clientConfig().pkceEnabled(),                                   bool())
    );

    private static final Encoder<Group, Map<String, Object>> GROUP_REF = object(
        property("id",   Group::id,             long_()),
        property("name", g -> g.name().value(), string())
    );

    private static final Encoder<Role, Map<String, Object>> ROLE_REF = object(
        property("id",   Role::id,             long_()),
        property("name", r -> r.name().value(), string())
    );

    private static final Encoder<Realm, Map<String, Object>> REALM_REF = object(
        property("id",   Realm::id,             long_()),
        property("name", r -> r.name().value(), string())
    );

    public static final Encoder<Assignment, Map<String, Object>> ASSIGNMENT = object(
        property("group", Assignment::group, nested(GROUP_REF)),
        property("role",  Assignment::role,  nested(ROLE_REF)),
        property("realm", Assignment::realm, nested(REALM_REF))
    );

    public static final Encoder<UserSession, Map<String, Object>> USER_SESSION = object(
        property("token",          UserSession::token,         string()),
        property("remote_address", UserSession::remoteAddress, nullable(string())),
        property("user_agent",     UserSession::userAgent,     nullable(string())),
        property("created_at",     s -> s.createdAt() != null ? s.createdAt().toString() : null, nullable(string()))
    );

    public static final Encoder<UserAction, Map<String, Object>> USER_ACTION = object(
        property("id",          UserAction::id,                                                 long_()),
        property("action_type", a -> a.actionType() != null ? a.actionType().name() : null,    nullable(string())),
        property("actor",       UserAction::actor,                                              nullable(string())),
        property("actor_ip",    UserAction::actorIp,                                            nullable(string())),
        property("options",     UserAction::options,                                            nullable(string())),
        property("created_at",  a -> a.createdAt() != null ? a.createdAt().toString() : null,  nullable(string()))
    );

    private static final Encoder<GroupInvitation, Map<String, Object>> GROUP_INVITATION = object(
        property("id",    GroupInvitation::id,    long_()),
        property("group", GroupInvitation::group, nested(GROUP_REF))
    );

    private static final Encoder<OidcInvitation, Map<String, Object>> OIDC_INVITATION = object(
        property("id",           OidcInvitation::id,          long_()),
        property("oidcProvider", OidcInvitation::oidcProvider, nested(OIDC_PROVIDER)),
        property("oidcPayload",  OidcInvitation::oidcPayload, nullable(string()))
    );

    public static final Encoder<Invitation, Map<String, Object>> INVITATION = object(
        property("id",               Invitation::id,    long_()),
        property("code",             Invitation::code,  string()),
        property("email",            Invitation::email, nullable(string())),
        property("invitedAt",        i -> i.invitedAt() != null ? i.invitedAt().toString() : null, nullable(string())),
        property("groupInvitations", i -> i.groupInvitations() != null ? i.groupInvitations() : List.<GroupInvitation>of(), list(nested(GROUP_INVITATION))),
        property("oidcInvitations",  i -> i.oidcInvitations()  != null ? i.oidcInvitations()  : List.<OidcInvitation>of(),  list(nested(OIDC_INVITATION)))
    );

    private BouncrJsonEncoders() {}
}
