package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.User;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class GroupRepository {
    private final DSLContext dsl;

    public GroupRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Group> findByName(String name, boolean embedUsers) {
        var rec = dsl.select(
                        field("group_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .from(table("groups"))
                .where(field("name").eq(name))
                .fetchOne();
        if (rec == null) return Optional.empty();

        List<User> users = embedUsers
                ? findUsersByGroupId(rec.get(field("group_id", Long.class)))
                : null;
        Group group = GROUP.decode(rec).getOrThrow();
        return Optional.of(users != null ? new Group(group.id(), group.name(), group.description(), group.writeProtected(), users) : group);
    }

    public List<Group> search(String q, Long userId, boolean isAdmin, int offset, int limit) {
        var query = dsl.selectDistinct(
                        field("g.group_id", Long.class).as("group_id"),
                        field("g.name", String.class).as("name"),
                        field("g.description", String.class).as("description"),
                        field("g.write_protected", Boolean.class).as("write_protected"))
                .from(table("groups").as("g"));

        if (!isAdmin) {
            query = query
                    .join(table("memberships").as("m")).on(field("m.group_id").eq(field("g.group_id")));
        }

        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            String likeExpr = "%" + q.replace("%", "\\%") + "%";
            condition = condition.and(field("g.name", String.class).like(likeExpr));
        }
        if (!isAdmin) {
            condition = condition.and(field("m.user_id").eq(userId));
        }

        return query.where(condition)
                .orderBy(field("g.group_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> GROUP.decode(rec).getOrThrow());
    }

    public boolean isNameUnique(String name) {
        return dsl.selectCount()
                .from(table("groups"))
                .where(field("name_lower").eq(name.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public Group insert(String name, String description) {
        Record rec = dsl.insertInto(table("groups"),
                        field("name"), field("name_lower"), field("description"), field("write_protected"))
                .values(name, name.toLowerCase(Locale.US), description, false)
                .returningResult(
                        field("group_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return GROUP.decode(rec).getOrThrow();
    }

    public void update(String currentName, String newName, String description) {
        var updateSet = dsl.update(table("groups"))
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
        dsl.deleteFrom(table("groups"))
                .where(field("name").eq(name))
                .execute();
    }

    public void addUser(String groupName, Long userId) {
        Long groupId = dsl.select(field("group_id", Long.class))
                .from(table("groups"))
                .where(field("name").eq(groupName))
                .fetchOne(field("group_id", Long.class));

        dsl.insertInto(table("memberships"),
                        field("user_id"), field("group_id"))
                .values(userId, groupId)
                .execute();
    }

    public void removeUser(String groupName, Long userId) {
        Long groupId = dsl.select(field("group_id", Long.class))
                .from(table("groups"))
                .where(field("name").eq(groupName))
                .fetchOne(field("group_id", Long.class));

        dsl.deleteFrom(table("memberships"))
                .where(field("user_id").eq(userId)
                        .and(field("group_id").eq(groupId)))
                .execute();
    }

    private List<User> findUsersByGroupId(Long groupId) {
        return dsl.select(
                        field("u.user_id", Long.class).as("user_id"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("users").as("u"))
                .join(table("memberships").as("m")).on(field("m.user_id").eq(field("u.user_id")))
                .where(field("m.group_id").eq(groupId))
                .fetch(rec -> USER.decode(rec).getOrThrow());
    }
}
