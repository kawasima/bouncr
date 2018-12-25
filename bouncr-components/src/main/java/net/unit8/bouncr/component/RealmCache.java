package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.component.jpa.EntityManagerProvider;
import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.entity.Application;
import net.unit8.bouncr.entity.Realm;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RealmCache extends SystemComponent<RealmCache> {
    @Inject
    private EntityManagerProvider entityManagerProvider;
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
                if (realmCache.applications != null) {
                    realmCache.applications.clear();
                }
                if (realmCache.cache != null) {
                    realmCache.cache.clear();
                }
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
                .filter(app -> app.equals(realm.getApplication()))
                .findFirst()
                .orElseThrow(() -> new MisconfigurationException("bouncr.APPLICATION_NOT_FOUND", realm.getApplication().getId()));
    }

    public synchronized void refresh() {
        EntityManager em = entityManagerProvider.createEntityManager();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Application> query = builder.createQuery(Application.class);
        query.from(Application.class);
        applications = em.createQuery(query).getResultList();

        CriteriaQuery<Realm> realmQuery = builder.createQuery(Realm.class);
        Root<Realm> realmRoot = realmQuery.from(Realm.class);
        cache = em.createQuery(realmQuery).getResultList()
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
