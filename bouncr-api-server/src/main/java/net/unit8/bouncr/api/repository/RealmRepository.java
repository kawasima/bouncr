package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Realm;
import net.unit8.bouncr.data.RealmSpec;
import net.unit8.bouncr.data.WordName;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class RealmRepository {
    private final DSLContext dsl;

    public RealmRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Realm> findByApplicationAndName(WordName appName, String realmName) {
        var rec = dsl.select(
                        field("r.realm_id", Long.class).as("realm_id"),
                        field("r.name", String.class).as("name"),
                        field("r.url", String.class).as("url"),
                        field("r.description", String.class).as("description"),
                        field("r.write_protected", Boolean.class).as("write_protected"))
                .from(table("realms").as("r"))
                .join(table("applications").as("a")).on(field("a.application_id").eq(field("r.application_id")))
                .where(field("a.name").eq(appName.value())
                        .and(field("r.name").eq(realmName)))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(REALM.decode(rec).getOrThrow());
    }

    public List<Realm> search(WordName appName, String q, int offset, int limit) {
        var condition = noCondition();
        if (appName != null) {
            condition = condition.and(field("a.name").eq(appName.value()));
        }
        if (q != null && !q.isEmpty()) {
            condition = condition.and(LikeQuery.contains(field("r.name", String.class), q));
        }

        return dsl.select(
                        field("r.realm_id", Long.class).as("realm_id"),
                        field("r.name", String.class).as("name"),
                        field("r.url", String.class).as("url"),
                        field("r.description", String.class).as("description"),
                        field("r.write_protected", Boolean.class).as("write_protected"))
                .from(table("realms").as("r"))
                .join(table("applications").as("a")).on(field("a.application_id").eq(field("r.application_id")))
                .where(condition)
                .orderBy(field("r.realm_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> REALM.decode(rec).getOrThrow());
    }

    public Realm insert(Long applicationId, RealmSpec spec) {
        Record rec = dsl.insertInto(table("realms"),
                        field("application_id"), field("name"), field("name_lower"),
                        field("url"), field("description"), field("write_protected"))
                .values(applicationId, spec.name().value(), spec.name().lowercase(),
                        spec.url(), spec.description(), false)
                .returningResult(
                        field("realm_id", Long.class),
                        field("name", String.class),
                        field("url", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return REALM.decode(rec).getOrThrow();
    }

    public void update(Long applicationId, WordName currentName, RealmSpec spec) {
        var updateSet = dsl.update(table("realms"))
                .set(field("name"), (Object) (spec.name() != null ? spec.name().value() : currentName.value()));
        if (spec.name() != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) spec.name().lowercase());
        }
        if (spec.url() != null) {
            updateSet = updateSet.set(field("url"), (Object) spec.url());
        }
        if (spec.description() != null) {
            updateSet = updateSet.set(field("description"), (Object) spec.description());
        }
        updateSet.where(field("application_id").eq(applicationId)
                        .and(field("name").eq(currentName.value())))
                .execute();
    }

    public void delete(Long applicationId, WordName name) {
        dsl.deleteFrom(table("realms"))
                .where(field("application_id").eq(applicationId)
                        .and(field("name").eq(name.value())))
                .execute();
    }
}
