package net.unit8.bouncr.api.encoder;

import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Group;
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
        property("write_protected", Permission::writeProtected, bool())
    );

    public static final Encoder<Role, Map<String, Object>> ROLE = object(
        property("id",              Role::id,                   long_()),
        property("name",            r -> r.name().value(),      string()),
        property("description",     Role::description,          nullable(string())),
        property("write_protected", Role::writeProtected,       bool())
    );

    public static final Encoder<Group, Map<String, Object>> GROUP = object(
        property("id",              Group::id,                  long_()),
        property("name",            g -> g.name().value(),      string()),
        property("description",     Group::description,         nullable(string())),
        property("write_protected", Group::writeProtected,      bool())
    );

    public static final Encoder<Realm, Map<String, Object>> REALM = object(
        property("id",              Realm::id,                  long_()),
        property("name",            r -> r.name().value(),      string()),
        property("url",             Realm::url,                 nullable(string())),
        property("description",     Realm::description,         nullable(string())),
        property("write_protected", Realm::writeProtected,      bool())
    );

    public static final Encoder<Application, Map<String, Object>> APPLICATION = object(
        property("id",              Application::id,            long_()),
        property("name",            a -> a.name().value(),      string()),
        property("description",     Application::description,   nullable(string())),
        property("pass_to",         Application::passTo,        nullable(string())),
        property("virtual_path",    Application::virtualPath,   nullable(string())),
        property("top_page",        Application::topPage,       nullable(string())),
        property("write_protected", Application::writeProtected, bool())
    );

    private BouncrJsonEncoders() {}
}
