package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Realm;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class RealmRepository {
    private final DSLContext dsl;

    public RealmRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Realm> findByApplicationAndName(String appName, String realmName) {
        var rec = dsl.select(
                        field("r.realm_id", Long.class).as("realm_id"),
                        field("r.name", String.class).as("name"),
                        field("r.name_lower", String.class).as("name_lower"),
                        field("r.url", String.class).as("url"),
                        field("r.description", String.class).as("description"),
                        field("r.application_id", Long.class).as("application_id"),
                        field("r.write_protected", Boolean.class).as("write_protected"))
                .from(table("realms").as("r"))
                .join(table("applications").as("a")).on(field("a.application_id").eq(field("r.application_id")))
                .where(field("a.name").eq(appName)
                        .and(field("r.name").eq(realmName)))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(REALM.decode(rec).getOrThrow());
    }

    public List<Realm> search(String appName, String q, int offset, int limit) {
        var condition = noCondition();
        if (appName != null && !appName.isEmpty()) {
            condition = condition.and(field("a.name").eq(appName));
        }
        if (q != null && !q.isEmpty()) {
            String likeExpr = "%" + q.replace("%", "\\%") + "%";
            condition = condition.and(field("r.name", String.class).like(likeExpr));
        }

        return dsl.select(
                        field("r.realm_id", Long.class).as("realm_id"),
                        field("r.name", String.class).as("name"),
                        field("r.name_lower", String.class).as("name_lower"),
                        field("r.url", String.class).as("url"),
                        field("r.description", String.class).as("description"),
                        field("r.application_id", Long.class).as("application_id"),
                        field("r.write_protected", Boolean.class).as("write_protected"))
                .from(table("realms").as("r"))
                .join(table("applications").as("a")).on(field("a.application_id").eq(field("r.application_id")))
                .where(condition)
                .orderBy(field("r.realm_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> REALM.decode(rec).getOrThrow());
    }

    public Realm insert(Long applicationId, String name, String url, String description) {
        Record rec = dsl.insertInto(table("realms"),
                        field("application_id"), field("name"), field("name_lower"),
                        field("url"), field("description"), field("write_protected"))
                .values(applicationId, name, name.toLowerCase(Locale.US), url, description, false)
                .returningResult(
                        field("realm_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("url", String.class),
                        field("description", String.class),
                        field("application_id", Long.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return REALM.decode(rec).getOrThrow();
    }

    public void update(Long applicationId, String currentName, String newName, String url, String description) {
        var updateSet = dsl.update(table("realms"))
                .set(field("name"), (Object) (newName != null ? newName : field("name")));
        if (newName != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) newName.toLowerCase(Locale.US));
        }
        if (url != null) {
            updateSet = updateSet.set(field("url"), (Object) url);
        }
        if (description != null) {
            updateSet = updateSet.set(field("description"), (Object) description);
        }
        updateSet.where(field("application_id").eq(applicationId)
                        .and(field("name").eq(currentName)))
                .execute();
    }

    public void delete(Long applicationId, String name) {
        dsl.deleteFrom(table("realms"))
                .where(field("application_id").eq(applicationId)
                        .and(field("name").eq(name)))
                .execute();
    }
}
