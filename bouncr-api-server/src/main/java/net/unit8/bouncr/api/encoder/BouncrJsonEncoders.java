package net.unit8.bouncr.api.encoder;

import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.OidcProvider;
import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.Realm;
import net.unit8.bouncr.data.Role;
import net.unit8.raoh.encoder.Encoder;

import java.util.Map;

import static net.unit8.raoh.encoder.MapEncoders.*;
import static net.unit8.raoh.encoder.ObjectEncoders.*;

public final class BouncrJsonEncoders {

    public static final Encoder<Permission, Map<String, Object>> PERMISSION = object(
        property("id",              Permission::id,             long_()),
        property("name",            p -> p.name().value(),      string()),
        property("description",     Permission::description,    nullable(string())),
        property("writeProtected", Permission::writeProtected, bool())
    );

    public static final Encoder<Role, Map<String, Object>> ROLE = object(
        property("id",              Role::id,                   long_()),
        property("name",            r -> r.name().value(),      string()),
        property("description",     Role::description,          nullable(string())),
        property("writeProtected", Role::writeProtected,       bool())
    );

    public static final Encoder<Group, Map<String, Object>> GROUP = object(
        property("id",              Group::id,                  long_()),
        property("name",            g -> g.name().value(),      string()),
        property("description",     Group::description,         nullable(string())),
        property("writeProtected", Group::writeProtected,      bool())
    );

    public static final Encoder<Realm, Map<String, Object>> REALM = object(
        property("id",              Realm::id,                  long_()),
        property("name",            r -> r.name().value(),      string()),
        property("url",             Realm::url,                 nullable(string())),
        property("description",     Realm::description,         nullable(string())),
        property("writeProtected", Realm::writeProtected,      bool())
    );

    public static final Encoder<Application, Map<String, Object>> APPLICATION = object(
        property("id",              Application::id,            long_()),
        property("name",            a -> a.name().value(),      string()),
        property("description",     Application::description,   nullable(string())),
        property("pass_to",         Application::passTo,        nullable(string())),
        property("virtual_path",    Application::virtualPath,   nullable(string())),
        property("top_page",        Application::topPage,       nullable(string())),
        property("writeProtected", Application::writeProtected, bool())
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

    private BouncrJsonEncoders() {}
}
