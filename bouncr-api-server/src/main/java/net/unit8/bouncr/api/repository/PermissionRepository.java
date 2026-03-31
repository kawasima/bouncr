package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Permission;
import net.unit8.bouncr.data.PermissionName;
import net.unit8.bouncr.data.PermissionSpec;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class PermissionRepository {
    private final DSLContext dsl;

    public PermissionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Permission> findByName(String name) {
        return dsl.select(
                        field("permission_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .from(table("permissions"))
                .where(field("name").eq(name))
                .fetchOptional(rec -> PERMISSION.decode(rec).getOrThrow());
    }

    public List<Permission> search(String q, Long userId, boolean isAdmin, int offset, int limit) {
        var query = dsl.selectDistinct(
                        field("p.permission_id", Long.class).as("permission_id"),
                        field("p.name", String.class).as("name"),
                        field("p.description", String.class).as("description"),
                        field("p.write_protected", Boolean.class).as("write_protected"))
                .from(table("permissions").as("p"));

        if (!isAdmin) {
            query = query
                    .join(table("role_permissions").as("rp")).on(field("rp.permission_id").eq(field("p.permission_id")))
                    .join(table("assignments").as("a")).on(field("a.role_id").eq(field("rp.role_id")))
                    .join(table("memberships").as("m")).on(field("m.group_id").eq(field("a.group_id")));
        }

        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            condition = condition.and(LikeQuery.contains(field("p.name", String.class), q));
        }
        if (!isAdmin) {
            condition = condition.and(field("m.user_id").eq(userId));
        }

        return query.where(condition)
                .orderBy(field("p.permission_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> PERMISSION.decode(rec).getOrThrow());
    }

    public boolean isNameUnique(PermissionName name) {
        return dsl.selectCount()
                .from(table("permissions"))
                .where(field("name_lower").eq(name.lowercase()))
                .fetchOne(0, int.class) == 0;
    }

    public Permission insert(PermissionSpec spec) {
        Record rec = dsl.insertInto(table("permissions"),
                        field("name"), field("name_lower"), field("description"), field("write_protected"))
                .values(spec.name().value(), spec.name().lowercase(), spec.description(), false)
                .returningResult(
                        field("permission_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return PERMISSION.decode(rec).getOrThrow();
    }

    public void update(PermissionName currentName, PermissionSpec spec) {
        var updateSet = dsl.update(table("permissions"))
                .set(field("name"), (Object) (spec.name() != null ? spec.name().value() : currentName.value()));
        if (spec.name() != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) spec.name().lowercase());
        }
        if (spec.description() != null) {
            updateSet = updateSet.set(field("description"), (Object) spec.description());
        }
        updateSet.where(field("name").eq(currentName.value()))
                .execute();
    }

    public void delete(PermissionName name) {
        dsl.deleteFrom(table("permissions"))
                .where(field("name").eq(name.value()))
                .execute();
    }

    public List<Long> findIdsByNames(List<String> names) {
        return dsl.select(field("permission_id", Long.class))
                .from(table("permissions"))
                .where(field("name").in(names))
                .fetch(field("permission_id", Long.class));
    }
}
