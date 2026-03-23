package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.Role;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class RoleRepository {
    private final DSLContext dsl;

    public RoleRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Role> findByName(String name, boolean embedPermissions) {
        var rec = dsl.select(
                        field("role_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .from(table("roles"))
                .where(field("name").eq(name))
                .fetchOne();
        if (rec == null) return Optional.empty();

        List<Permission> permissions = embedPermissions
                ? findPermissionsByRoleId(rec.get(field("role_id", Long.class)))
                : null;
        Role role = ROLE.decode(rec).getOrThrow();
        return Optional.of(permissions != null ? new Role(role.id(), role.name(), role.description(), role.writeProtected(), permissions) : role);
    }

    public List<Role> search(String q, Long userId, boolean isAdmin, int offset, int limit) {
        var query = dsl.selectDistinct(
                        field("r.role_id", Long.class).as("role_id"),
                        field("r.name", String.class).as("name"),
                        field("r.description", String.class).as("description"),
                        field("r.write_protected", Boolean.class).as("write_protected"))
                .from(table("roles").as("r"));

        if (!isAdmin) {
            query = query
                    .join(table("assignments").as("a")).on(field("a.role_id").eq(field("r.role_id")))
                    .join(table("memberships").as("m")).on(field("m.group_id").eq(field("a.group_id")));
        }

        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            condition = condition.and(LikeQuery.contains(field("r.name", String.class), q));
        }
        if (!isAdmin) {
            condition = condition.and(field("m.user_id").eq(userId));
        }

        return query.where(condition)
                .orderBy(field("r.role_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> ROLE.decode(rec).getOrThrow());
    }

    public boolean isNameUnique(String name) {
        return dsl.selectCount()
                .from(table("roles"))
                .where(field("name_lower").eq(name.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public Role insert(String name, String description) {
        Record rec = dsl.insertInto(table("roles"),
                        field("name"), field("name_lower"), field("description"), field("write_protected"))
                .values(name, name.toLowerCase(Locale.US), description, false)
                .returningResult(
                        field("role_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return ROLE.decode(rec).getOrThrow();
    }

    public void update(String currentName, String newName, String description) {
        var updateSet = dsl.update(table("roles"))
                .set(field("name"), (Object) (newName != null ? newName : field("name")));
        if (newName != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) newName.toLowerCase(Locale.US));
        }
        if (description != null) {
            updateSet = updateSet.set(field("description"), (Object) description);
        }
        updateSet.where(field("name").eq(currentName))
                .execute();
    }

    public void delete(String name) {
        dsl.deleteFrom(table("roles"))
                .where(field("name").eq(name))
                .execute();
    }

    private List<Permission> findPermissionsByRoleId(Long roleId) {
        return dsl.select(
                        field("p.permission_id", Long.class).as("permission_id"),
                        field("p.name", String.class).as("name"),
                        field("p.description", String.class).as("description"),
                        field("p.write_protected", Boolean.class).as("write_protected"))
                .from(table("permissions").as("p"))
                .join(table("role_permissions").as("rp")).on(field("rp.permission_id").eq(field("p.permission_id")))
                .where(field("rp.role_id").eq(roleId))
                .fetch(rec -> PERMISSION.decode(rec).getOrThrow());
    }
}
