package net.unit8.bouncr.hook.license;

import enkan.collection.Headers;
import enkan.data.Cookie;
import enkan.data.jpa.EntityManageable;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.UserLicense;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static enkan.util.ThreadingUtils.some;

public class LicenseDeleteHook implements Hook<RestContext> {
    private LicenseConfig config;

    public LicenseDeleteHook(LicenseConfig config) {
        this.config = config;
    }

    @Override
    public void run(RestContext context) {
        final EntityManager em = ((EntityManageable) context.getRequest()).getEntityManager();
        LicenseKey licenseKey = some(context.getRequest().getCookies(),
                c->c.get(config.getCookieName()),
                Cookie::getValue,
                LicenseKey::new).orElse(null);

        if (licenseKey == null) return;

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<UserLicense> query = cb.createQuery(UserLicense.class);
        final Root<UserLicense> root = query.from(UserLicense.class);
        query.where(cb.equal(root.get("licenseKey"), licenseKey.asBytes()));
        em.createQuery(query).getResultStream()
                .forEach(em::remove);

        Cookie cookie = Cookie.create(config.getCookieName(), licenseKey.asString());
        ZoneId zone = ZoneId.systemDefault();
        Date expires = Date.from(
                ZonedDateTime.of(LocalDate.now()
                        .minusYears(10)
                        .atTime(0, 0), zone)
                        .toInstant());
        cookie.setExpires(expires);
        cookie.setPath("/");
        context.setHeaders(Headers.of("Set-Cookie", cookie.toHttpString()));

    }
}
