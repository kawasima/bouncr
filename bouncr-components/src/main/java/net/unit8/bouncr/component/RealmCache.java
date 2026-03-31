package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.component.jooq.JooqProvider;
import jakarta.inject.Inject;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.jooq.JooqRecordDecoders;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.unit8.raoh.decode.Decoders.combine;
import static net.unit8.raoh.decode.ObjectDecoders.long_;
import static net.unit8.raoh.decode.ObjectDecoders.string;
import static org.jooq.impl.DSL.*;

public class RealmCache extends SystemComponent<RealmCache> {
    @Inject
    private JooqProvider jooqProvider;

    private List<CachedRealm> cache;

    public record CachedRealm(Long realmId, String name, String url, Pattern urlPattern,
                               Long applicationId, String virtualPath, String passTo) {
    }

    // Unquoted field references — compatible with both H2 (uppercase) and PostgreSQL (lowercase)
    private static final Field<Long> R_REALM_ID = field("r.realm_id", Long.class);
    private static final Field<String> R_NAME = field("r.name", String.class);
    private static final Field<String> R_URL = field("r.url", String.class);
    private static final Field<Long> R_APP_ID = field("r.application_id", Long.class);
    private static final Field<Long> A_APP_ID = field("a.application_id", Long.class);
    private static final Field<String> A_VIRTUAL_PATH = field("a.virtual_path", String.class);
    private static final Field<String> A_PASS_TO = field("a.pass_to", String.class);

    private static final Decoder<Record, CachedRealm> CACHED_REALM_DECODER = combine(
            JooqRecordDecoders.field("realm_id", long_()),
            JooqRecordDecoders.field("name", string()),
            JooqRecordDecoders.field("url", string()),
            JooqRecordDecoders.field("application_id", long_()),
            JooqRecordDecoders.field("virtual_path", string()),
            JooqRecordDecoders.field("pass_to", string())
    ).map((realmId, name, url, appId, virtualPath, passTo) ->
            new CachedRealm(realmId, name, url, Pattern.compile("^" + url + "$"),
                    appId, virtualPath, passTo));

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
        if (path == null) return null;
        return cache.stream()
                .filter(realm -> {
                    String vp = realm.virtualPath();
                    if (vp == null) return false;

                    if (path.equals(vp)) {
                        // Exact virtualPath match — remainder is ""
                        return realm.urlPattern().matcher("").matches();
                    }

                    String prefix = vp.endsWith("/") ? vp : vp + "/";
                    if (path.startsWith(prefix)) {
                        String remainder = path.substring(prefix.length());
                        return realm.urlPattern().matcher(remainder).matches();
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    public synchronized void refresh() {
        DSLContext dsl = jooqProvider.getDSLContext();

        cache = dsl.select(
                        R_REALM_ID.as("realm_id"),
                        R_NAME.as("name"),
                        R_URL.as("url"),
                        A_APP_ID.as("application_id"),
                        A_VIRTUAL_PATH.as("virtual_path"),
                        A_PASS_TO.as("pass_to"))
                .from(table("realms").as("r"))
                .join(table("applications").as("a")).on(R_APP_ID.eq(A_APP_ID))
                .fetch()
                .stream()
                .map(rec -> CACHED_REALM_DECODER.decode(rec).getOrThrow())
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
