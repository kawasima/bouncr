package net.unit8.bouncr.hook.license;

import enkan.data.Cookie;
import enkan.data.jpa.EntityManageable;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.UserLicense;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static enkan.util.ThreadingUtils.some;

public class LicenseDeleteHook implements Hook<RestContext> {
    private LicenseConfig config;

    public LicenseDeleteHook(LicenseConfig config) {
        this.config = config;
    }

    @Override
    public void run(RestContext context) {
        final EntityManager em = ((EntityManageable) context.getRequest()).getEntityManager();
        context.getValue(User.class).ifPresent(user -> {
            final LicenseKey licenseKey = some(context.getRequest().getCookies(),
                    c->c.get(config.getCookieName()),
                    Cookie::getValue,
                    LicenseKey::new).orElse(null);

            if (licenseKey == null) return;

            final CriteriaBuilder cb = em.getCriteriaBuilder();
            final CriteriaQuery<UserLicense> query = cb.createQuery(UserLicense.class);
            final Root<UserLicense> root = query.from(UserLicense.class);
            query.where(cb.equal(root.get("user"), user),
                    cb.equal(root.get("licenseKey"), licenseKey.asBytes()));
            em.createQuery(query).getResultStream()
                    .forEach(em::remove);
        });

    }
}
