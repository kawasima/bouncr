package net.unit8.bouncr.hook.license;

import enkan.data.Cookie;
import enkan.data.jpa.EntityManageable;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.LicenseLastActivity;
import net.unit8.bouncr.hook.license.entity.UserLicense;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static enkan.util.BeanBuilder.builder;

public class LicenseCheckHook implements Hook<RestContext> {
    private static final URI EXCEED_LICENSING_DEVICES = URI.create("/bouncr/problem/EXCEED_LICENSING_DEVICES");
    private LicenseConfig config;

    public LicenseCheckHook(LicenseConfig config) {
        this.config = config;
    }

    private LicenseLastActivity findLastActivity(EntityManager em, UserLicense userLicense) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<LicenseLastActivity> query = cb.createQuery(LicenseLastActivity.class);
        final Root<LicenseLastActivity> root = query.from(LicenseLastActivity.class);
        query.where(cb.equal(root.get("userLicense"), userLicense));
        return em.createQuery(query)
                .getResultStream()
                .findAny()
                .orElse(null);
    }
    @Override
    public void run(RestContext context) {
        final EntityManager em = ((EntityManageable) context.getRequest()).getEntityManager();
        context.getValue(User.class).ifPresent(user -> {
            final LicenseKey licenseKey = Optional.ofNullable(context.getRequest().getCookies())
                    .map(c -> c.get(config.getCookieName()))
                    .map(Cookie::getValue)
                    .map(LicenseKey::new)
                    .orElse(null);

            final CriteriaBuilder cb = em.getCriteriaBuilder();
            final CriteriaQuery<UserLicense> query = cb.createQuery(UserLicense.class);
            final Root<UserLicense> root = query.from(UserLicense.class);
            query.where(cb.equal(root.get("user"), user));
            final List<UserLicense> licenses = em.createQuery(query).getResultList();
            UserLicense userLicense = licenses.stream()
                    .filter(lic-> new LicenseKey(lic.getLicenseKey()).equals(licenseKey))
                    .findAny()
                    .orElse(null);
            if (userLicense == null
                    && licenses.size() >= config.getNumOfDevicesPerUser()) {
                Problem problem = Problem.valueOf(403, EXCEED_LICENSING_DEVICES);
                context.setMessage(problem);
            } else {
                if (userLicense != null) {
                    final LicenseLastActivity lastActivity = findLastActivity(em, userLicense);
                    final EntityTransactionManager tx = new EntityTransactionManager(em);
                    if (lastActivity == null) {
                        tx.required(() -> {
                            em.persist(builder(new LicenseLastActivity())
                                    .set(LicenseLastActivity::setUserLicense, userLicense)
                                    .set(LicenseLastActivity::setUserAgent, Optional.ofNullable(context.getRequest().getHeaders())
                                            .map(headers -> headers.get("User-Agent"))
                                            .map(ua -> ua.substring(0, Math.min(ua.length()-1, 255)))
                                            .orElse(null))
                                    .set(LicenseLastActivity::setLastUsedAt, LocalDateTime.now())
                                    .build());
                        });
                    } else {
                        tx.required(() -> {
                            lastActivity.setUserAgent(Optional.ofNullable(context.getRequest().getHeaders())
                                    .map(headers -> headers.get("User-Agent"))
                                    .map(ua -> ua.substring(0, Math.min(ua.length()-1, 255)))
                                    .orElse(null));
                            lastActivity.setLastUsedAt(LocalDateTime.now());
                        });
                    }
                    context.putValue(userLicense);
                }
                context.putValue(licenseKey);
            }
        });
    }
}
