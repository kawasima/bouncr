package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Realm;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.*;
import java.util.stream.Collectors;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class ApplicationRepository {
    private final DSLContext dsl;

    public ApplicationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Application> findByName(String name, boolean embedRealms) {
        var rec = dsl.select(
                        field("application_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("pass_to", String.class),
                        field("virtual_path", String.class),
                        field("top_page", String.class),
                        field("write_protected", Boolean.class))
                .from(table("applications"))
                .where(field("name").eq(name))
                .fetchOne();
        if (rec == null) return Optional.empty();

        Application app = APPLICATION.decode(rec).getOrThrow();
        List<Realm> realms = embedRealms ? findRealmsByApplicationId(rec.get(field("application_id", Long.class))) : null;
        return Optional.of(realms != null ? new Application(app.id(), app.name(), app.description(), app.passTo(), app.virtualPath(), app.topPage(), app.writeProtected(), realms) : app);
    }

    public List<Application> search(String q, boolean embedRealms, int offset, int limit) {
        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            condition = condition.and(LikeQuery.contains(field("name", String.class), q));
        }

        var apps = dsl.select(
                        field("application_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("pass_to", String.class),
                        field("virtual_path", String.class),
                        field("top_page", String.class),
                        field("write_protected", Boolean.class))
                .from(table("applications"))
                .where(condition)
                .orderBy(field("application_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch();

        if (!embedRealms) {
            return apps.map(rec -> APPLICATION.decode(rec).getOrThrow());
        }

        Set<Long> appIds = apps.stream()
                .map(r -> r.get(field("application_id", Long.class)))
                .collect(Collectors.toSet());

        Map<Long, List<Realm>> realmsByAppId = loadRealmsByApplicationIds(appIds);

        return apps.map(rec -> {
            Application app = APPLICATION.decode(rec).getOrThrow();
            List<Realm> realms = realmsByAppId.getOrDefault(app.id(), List.of());
            return new Application(app.id(), app.name(), app.description(), app.passTo(), app.virtualPath(), app.topPage(), app.writeProtected(), realms);
        });
    }

    public boolean isNameUnique(String name) {
        return dsl.selectCount()
                .from(table("applications"))
                .where(field("name_lower").eq(name.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public Application insert(String name, String description, String virtualPath, String passTo, String topPage) {
        Record rec = dsl.insertInto(table("applications"),
                        field("name"), field("name_lower"), field("description"),
                        field("virtual_path"), field("pass_to"), field("top_page"), field("write_protected"))
                .values(name, name.toLowerCase(Locale.US), description, virtualPath, passTo, topPage, false)
                .returningResult(
                        field("application_id", Long.class),
                        field("name", String.class),
                        field("description", String.class),
                        field("pass_to", String.class),
                        field("virtual_path", String.class),
                        field("top_page", String.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return APPLICATION.decode(rec).getOrThrow();
    }

    public void update(String currentName, String newName, String description, String virtualPath, String passTo, String topPage) {
        var updateSet = dsl.update(table("applications"))
                .set(field("name"), (Object) (newName != null ? newName : field("name")));
        if (newName != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) newName.toLowerCase(Locale.US));
        }
        if (description != null) {
            updateSet = updateSet.set(field("description"), (Object) description);
        }
        if (virtualPath != null) {
            updateSet = updateSet.set(field("virtual_path"), (Object) virtualPath);
        }
        if (passTo != null) {
            updateSet = updateSet.set(field("pass_to"), (Object) passTo);
        }
        if (topPage != null) {
            updateSet = updateSet.set(field("top_page"), (Object) topPage);
        }
        updateSet.where(field("name").eq(currentName)).execute();
    }

    public void delete(String name) {
        dsl.deleteFrom(table("applications"))
                .where(field("name").eq(name))
                .execute();
    }

    private List<Realm> findRealmsByApplicationId(Long applicationId) {
        return dsl.select(
                        field("realm_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("url", String.class),
                        field("description", String.class),
                        field("write_protected", Boolean.class))
                .from(table("realms"))
                .where(field("application_id").eq(applicationId))
                .fetch(rec -> REALM.decode(rec).getOrThrow());
    }

    private Map<Long, List<Realm>> loadRealmsByApplicationIds(Set<Long> appIds) {
        if (appIds.isEmpty()) return Map.of();
        return dsl.select(
                        field("realm_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("url", String.class),
                        field("description", String.class),
                        field("application_id", Long.class),
                        field("write_protected", Boolean.class))
                .from(table("realms"))
                .where(field("application_id").in(appIds))
                .fetchGroups(
                        rec -> rec.get(field("application_id", Long.class)),
                        rec -> REALM.decode(rec).getOrThrow());
    }
}
