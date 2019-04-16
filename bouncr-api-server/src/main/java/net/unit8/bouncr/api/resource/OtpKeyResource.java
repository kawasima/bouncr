package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.OtpKey;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OtpKeyResource {
    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(HANDLE_OK)
    public OtpKey find(UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OtpKey> query = cb.createQuery(OtpKey.class);
        Root<OtpKey> root = query.from(OtpKey.class);
        Join<OtpKey, User> userRoot = root.join("user");
        query.where(cb.equal(userRoot.get("account"), principal.getName()));
        return em.createQuery(query).getResultStream().findAny()
                .orElse(new OtpKey());
    }

    @Decision(PUT)
    public OtpKey create(UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> userQuery = cb.createQuery(User.class);
        Root<User> userRoot = userQuery.from(User.class);
        userQuery.where(cb.equal(userRoot.get("account"), principal.getName()));
        User user = em.createQuery(userQuery).getResultStream().findAny().orElseThrow();
        OtpKey otpKey = builder(new OtpKey())
                .set(OtpKey::setKey, RandomUtils.generateRandomString(20, config.getSecureRandom()).getBytes())
                .set(OtpKey::setUser, user)
                .build();
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(otpKey));
        return otpKey;
    }

    @Decision(DELETE)
    public Void delete(UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OtpKey> query = cb.createQuery(OtpKey.class);
        Root<OtpKey> otpKeyRoot = query.from(OtpKey.class);
        Join<User, OtpKey> userRoot = otpKeyRoot.join("user");
        query.where(cb.equal(userRoot.get("account"), principal.getName()));
        EntityTransactionManager tx = new EntityTransactionManager(em);
        em.createQuery(query)
                .getResultStream()
                .findAny()
                .ifPresent(otpKey -> tx.required(() -> em.remove(otpKey)));
        return null;
    }
}
