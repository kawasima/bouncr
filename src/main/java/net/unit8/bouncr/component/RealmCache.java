package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.component.doma2.DomaProvider;
import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.web.dao.ApplicationDao;
import net.unit8.bouncr.web.dao.RealmDao;
import net.unit8.bouncr.web.entity.Application;
import net.unit8.bouncr.web.entity.Realm;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RealmCache extends SystemComponent<RealmCache> {
    @Inject
    private DomaProvider domaProvider;
    private List<Realm> cache;
    private List<Application> applications;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<RealmCache>() {
            @Override
            public void start(RealmCache realmCache) {
                realmCache.refresh();
            }

            @Override
            public void stop(RealmCache realmCache) {
                realmCache.applications.clear();
                realmCache.cache.clear();
            }
        };
    }

    public Realm matches(String path) {
        return cache.stream()
                .filter(realm -> realm.getPathPattern().matcher(path).matches())
                .findAny()
                .orElse(null);
    }

    public Application getApplication(Realm realm) {
        return applications.stream()
                .filter(app -> app.getId().equals(realm.getApplicationId()))
                .findFirst()
                .orElseThrow(() -> new MisconfigurationException("bouncr.APPLICATION_NOT_FOUND", realm.getApplicationId()));
    }

    public synchronized void refresh() {
        RealmDao realmDao = domaProvider.getDao(RealmDao.class);
        ApplicationDao applicationDao = domaProvider.getDao(ApplicationDao.class);
        applications = applicationDao.selectAll();
        cache = realmDao.selectAll()
                .stream()
                .map(realm -> {
                    Application app = getApplication(realm);
                    realm.setPathPattern(Pattern.compile("^" + app.getVirtualPath() + "($|/" + realm.getUrl() + ")"));
                    return realm;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "#RealmCache {\n"
                + "  \"cache\": " + cache + ","
                + "  \"applications\": " + applications + ","
                + "  \"dependencies\": " + dependenciesToString()
                + "\n}";
    }
}
