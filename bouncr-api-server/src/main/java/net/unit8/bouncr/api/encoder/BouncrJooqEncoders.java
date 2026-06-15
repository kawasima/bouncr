package net.unit8.bouncr.api.encoder;

import net.unit8.bouncr.data.ApplicationSpec;
import net.unit8.bouncr.data.GroupSpec;
import net.unit8.bouncr.data.PermissionSpec;
import net.unit8.bouncr.data.RealmSpec;
import net.unit8.bouncr.data.RoleSpec;
import net.unit8.raoh.encode.Encoder;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.unit8.raoh.encode.MapEncoders.*;
import static net.unit8.raoh.encode.ObjectEncoders.*;
import static org.jooq.impl.DSL.*;

/**
 * raoh {@link Encoder} instances that convert domain Spec objects
 * to {@code Map<String, Object>} for jOOQ INSERT operations.
 *
 * <p>Map keys are database column names (snake_case).
 * Compare with {@link BouncrJsonEncoders} where keys are JSON property names.
 */
public final class BouncrJooqEncoders {

    public static final Encoder<GroupSpec, Map<String, Object>> GROUP_SPEC = object(
        property("name",            s -> s.name().value(),     string()),
        property("name_lower",      s -> s.name().lowercase(), string()),
        property("description",     GroupSpec::description,    nullable(string())),
        property("write_protected", s -> false,                bool())
    );

    public static final Encoder<RoleSpec, Map<String, Object>> ROLE_SPEC = object(
        property("name",            s -> s.name().value(),     string()),
        property("name_lower",      s -> s.name().lowercase(), string()),
        property("description",     RoleSpec::description,     nullable(string())),
        property("write_protected", s -> false,                bool())
    );

    public static final Encoder<PermissionSpec, Map<String, Object>> PERMISSION_SPEC = object(
        property("name",            s -> s.name().value(),     string()),
        property("name_lower",      s -> s.name().lowercase(), string()),
        property("description",     PermissionSpec::description, nullable(string())),
        property("write_protected", s -> false,                bool())
    );

    public static final Encoder<ApplicationSpec, Map<String, Object>> APPLICATION_SPEC = object(
        property("name",            s -> s.name().value(),       string()),
        property("name_lower",      s -> s.name().lowercase(),   string()),
        property("description",     ApplicationSpec::description, nullable(string())),
        property("pass_to",         ApplicationSpec::passTo,     nullable(string())),
        property("virtual_path",    ApplicationSpec::virtualPath, nullable(string())),
        property("top_page",        ApplicationSpec::topPage,    nullable(string())),
        property("write_protected", s -> false,                  bool())
    );

    public static final Encoder<RealmSpec, Map<String, Object>> REALM_SPEC = object(
        property("name",            s -> s.name().value(),     string()),
        property("name_lower",      s -> s.name().lowercase(), string()),
        property("url",             RealmSpec::url,            nullable(string())),
        property("description",     RealmSpec::description,    nullable(string())),
        property("write_protected", s -> false,                bool())
    );

    /**
     * Executes an INSERT using an encoded Map and returns the result with the specified columns.
     */
    public static <T> Record insertInto(DSLContext dsl, String tableName,
                                         Encoder<T, Map<String, Object>> encoder, T value,
                                         Collection<org.jooq.Field<?>> returningFields) {
        return insertInto(dsl, tableName, encoder, value, Map.of(), returningFields);
    }

    /**
     * Executes an INSERT using an encoded Map plus extra columns and returns the result.
     *
     * @param extraColumns additional columns not covered by the encoder (e.g. foreign keys)
     */
    public static <T> Record insertInto(DSLContext dsl, String tableName,
                                         Encoder<T, Map<String, Object>> encoder, T value,
                                         Map<String, Object> extraColumns,
                                         Collection<org.jooq.Field<?>> returningFields) {
        var map = new LinkedHashMap<>(extraColumns);
        map.putAll(encoder.encode(value));
        return dsl.insertInto(table(tableName),
                        map.keySet().stream().map(DSL::field).toList())
                .values(map.values().toArray(new Object[0]))
                .returningResult(returningFields)
                .fetchOne();
    }

    private BouncrJooqEncoders() {}
}
