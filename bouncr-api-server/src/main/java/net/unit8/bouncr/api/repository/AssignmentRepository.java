package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Assignment;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.Realm;
import net.unit8.bouncr.data.Role;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class AssignmentRepository {
    private final DSLContext dsl;

    public AssignmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Assignment> findByGroupRoleRealm(String groupName, String roleName, String realmName) {
        var rec = dsl.select(
                        field("a.group_id", Long.class).as("group_id"),
                        field("a.role_id", Long.class).as("role_id"),
                        field("a.realm_id", Long.class).as("realm_id"),
                        field("g.name", String.class).as("group_name"),
                        field("g.description", String.class).as("group_description"),
                        field("g.write_protected", Boolean.class).as("group_write_protected"),
                        field("r.name", String.class).as("role_name"),
                        field("r.description", String.class).as("role_description"),
                        field("r.write_protected", Boolean.class).as("role_write_protected"),
                        field("re.name", String.class).as("realm_name"),
                        field("re.name_lower", String.class).as("realm_name_lower"),
                        field("re.url", String.class).as("realm_url"),
                        field("re.description", String.class).as("realm_description"),
                        field("re.write_protected", Boolean.class).as("realm_write_protected"))
                .from(table("assignments").as("a"))
                .join(table("groups").as("g")).on(field("g.group_id").eq(field("a.group_id")))
                .join(table("roles").as("r")).on(field("r.role_id").eq(field("a.role_id")))
                .join(table("realms").as("re")).on(field("re.realm_id").eq(field("a.realm_id")))
                .where(field("g.name").eq(groupName)
                        .and(field("r.name").eq(roleName))
                        .and(field("re.name").eq(realmName)))
                .fetchOne();
        if (rec == null) return Optional.empty();

        Assignment assignment = new Assignment(
                new Group(rec.get(field("group_id", Long.class)), rec.get(field("group_name", String.class)),
                        rec.get(field("group_description", String.class)), rec.get(field("group_write_protected", Boolean.class)), null),
                new Role(rec.get(field("role_id", Long.class)), rec.get(field("role_name", String.class)),
                        rec.get(field("role_description", String.class)), rec.get(field("role_write_protected", Boolean.class)), null),
                new Realm(rec.get(field("realm_id", Long.class)), rec.get(field("realm_name", String.class)),
                        rec.get(field("realm_name_lower", String.class)), rec.get(field("realm_url", String.class)),
                        rec.get(field("realm_description", String.class)), null, rec.get(field("realm_write_protected", Boolean.class)), null));
        return Optional.of(assignment);
    }

    public Long resolveIdByName(String tableName, String idColumn, String name) {
        return dsl.select(field(idColumn, Long.class))
                .from(table(tableName))
                .where(field("name").eq(name))
                .fetchOne(field(idColumn, Long.class));
    }

    public boolean exists(Long groupId, Long roleId, Long realmId) {
        return dsl.selectCount()
                .from(table("assignments"))
                .where(field("group_id").eq(groupId)
                        .and(field("role_id").eq(roleId))
                        .and(field("realm_id").eq(realmId)))
                .fetchOne(0, int.class) > 0;
    }

    public List<Assignment> findByRealm(Long realmId) {
        return dsl.select(
                        field("a.group_id", Long.class).as("group_id"),
                        field("a.role_id", Long.class).as("role_id"),
                        field("a.realm_id", Long.class).as("realm_id"),
                        field("g.name", String.class).as("group_name"),
                        field("g.description", String.class).as("group_description"),
                        field("g.write_protected", Boolean.class).as("group_write_protected"),
                        field("r.name", String.class).as("role_name"),
                        field("r.description", String.class).as("role_description"),
                        field("r.write_protected", Boolean.class).as("role_write_protected"))
                .from(table("assignments").as("a"))
                .join(table("groups").as("g")).on(field("g.group_id").eq(field("a.group_id")))
                .join(table("roles").as("r")).on(field("r.role_id").eq(field("a.role_id")))
                .where(field("a.realm_id").eq(realmId))
                .fetch(rec -> ASSIGNMENT.decode(rec).getOrThrow());
    }

    public void insert(Long groupId, Long roleId, Long realmId) {
        dsl.insertInto(table("assignments"),
                        field("group_id"), field("role_id"), field("realm_id"))
                .values(groupId, roleId, realmId)
                .execute();
    }

    public void delete(Long groupId, Long roleId, Long realmId) {
        dsl.deleteFrom(table("assignments"))
                .where(field("group_id").eq(groupId)
                        .and(field("role_id").eq(roleId))
                        .and(field("realm_id").eq(realmId)))
                .execute();
    }
}
