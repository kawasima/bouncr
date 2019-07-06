package net.unit8.bouncr.hook.license;

import enkan.data.Cookie;
import enkan.data.jpa.EntityManageable;
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
import java.util.List;
import java.util.Optional;

public class LicenseCheckHook implements Hook<RestContext> {
    private static final URI EXCEED_LICENSING_DEVICES = URI.create("/bouncr/problem/EXCEED_LICENSING_DEVICES");
    private LicenseConfig config;

    public LicenseCheckHook(LicenseConfig config) {
        this.config = config;
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
                    context.putValue(userLicense);
                }
                context.putValue(licenseKey);
            }
        });
    }
}
