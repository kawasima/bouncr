package net.unit8.bouncr.hook.license;

import enkan.data.Cookie;
import enkan.data.jpa.EntityManageable;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.UserLicense;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;

public class LicenseCheckHook implements Hook<RestContext> {
    private static final URI EXCEED_LICENSING_DEVICES = URI.create("/bouncr/problem/EXCEED_LICENSING_DEVICES");

    private int numOfDevicesPerUser = 3;
    private void saveLicenseKey(String licenseKey, User user, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        builder(new UserLicense())
                .set(UserLicense::setUser, user)
                .set(UserLicense::setLicenseKey, licenseKey)
                .build();
        tx.required(() -> em.persist(licenseKey));
    }

    private String publishLicenseKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void run(RestContext context) {
        final EntityManager em = ((EntityManageable) context.getRequest()).getEntityManager();
        context.getValue(User.class).ifPresent(user -> {
            final String licenseKey = some(context.getRequest().getParams(), p->p.get("device_key")).orElse("");
            final CriteriaBuilder cb = em.getCriteriaBuilder();
            final CriteriaQuery<UserLicense> query = cb.createQuery(UserLicense.class);
            final Root<UserLicense> root = query.from(UserLicense.class);
            query.where(cb.equal(root.get("user"), user));
            final List<UserLicense> licenses = em.createQuery(query).getResultList();
            if (licenses.stream().anyMatch(lic->lic.getLicenseKey().equals(licenseKey))) {
                return;
            } else if (licenses.size() < numOfDevicesPerUser) {
                String newLicenseKey = publishLicenseKey();
                saveLicenseKey(newLicenseKey, user, em);
                Cookie cookie = Cookie.create("BOUNCR_LICENSE_KEY", newLicenseKey);
                ZoneId zone = ZoneId.systemDefault();
                Date expires = Date.from(
                        ZonedDateTime.of(LocalDate.now()
                                .plusYears(10)
                                .atTime(0, 0), zone)
                                .toInstant());
                cookie.setExpires(expires);
                context.getRequest().setCookies(Map.of("BOUNCR_LICENSE_KEY", cookie));
            } else {
                Problem problem = Problem.valueOf(403, EXCEED_LICENSING_DEVICES);
                context.setMessage(problem);
            }
        });
    }

    public void setNumOfDevicesPerUser(int numOfDevicesPerUser) {
        this.numOfDevicesPerUser = numOfDevicesPerUser;
    }

}
