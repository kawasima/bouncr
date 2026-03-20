package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.component.jooq.JooqProvider;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

public class RealmCache extends SystemComponent<RealmCache> {
    @Inject
    private JooqProvider jooqProvider;

    private List<CachedRealm> cache;

    public record CachedRealm(Long realmId, String name, String url, Long applicationId,
                               String virtualPath, String passTo, Pattern pathPattern) {
    }

    // Unquoted field references — compatible with both H2 (uppercase) and PostgreSQL (lowercase)
    private static final Field<Long> R_REALM_ID = field("r.realm_id", Long.class);
    private static final Field<String> R_NAME = field("r.name", String.class);
    private static final Field<String> R_URL = field("r.url", String.class);
    private static final Field<Long> R_APP_ID = field("r.application_id", Long.class);
    private static final Field<Long> A_APP_ID = field("a.application_id", Long.class);
    private static final Field<String> A_VIRTUAL_PATH = field("a.virtual_path", String.class);
    private static final Field<String> A_PASS_TO = field("a.pass_to", String.class);

    @Override
    protected ComponentLifecycle<RealmCache> lifecycle() {
        return new ComponentLifecycle<RealmCache>() {
            @Override
            public void start(RealmCache realmCache) {
                realmCache.refresh();
            }

            @Override
            public void stop(RealmCache realmCache) {
                if (realmCache.cache != null) {
                    realmCache.cache.clear();
                }
            }
        };
    }

    public CachedRealm matches(String path) {
        return cache.stream()
                .filter(realm -> realm.pathPattern().matcher(path).matches())
                .findAny()
                .orElse(null);
    }

    public synchronized void refresh() {
        DSLContext dsl = jooqProvider.getDSLContext();

        cache = dsl.select(R_REALM_ID, R_NAME, R_URL, A_APP_ID, A_VIRTUAL_PATH, A_PASS_TO)
                .from(table("realms").as("r"))
                .join(table("applications").as("a")).on(R_APP_ID.eq(A_APP_ID))
                .fetch()
                .stream()
                .map(rec -> {
                    String virtualPath = rec.get(A_VIRTUAL_PATH);
                    String url = rec.get(R_URL);
                    Pattern pattern = Pattern.compile("^" + virtualPath + "($|/" + url + ")");
                    return new CachedRealm(
                            rec.get(R_REALM_ID),
                            rec.get(R_NAME),
                            url,
                            rec.get(A_APP_ID),
                            virtualPath,
                            rec.get(A_PASS_TO),
                            pattern);
                })
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "#RealmCache {\n"
                + "  \"cache\": " + cache + ","
                + "  \"dependencies\": " + dependenciesToString()
                + "\n}";
    }
}
