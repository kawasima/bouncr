package net.unit8.bouncr.api.resource.me;

import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import net.unit8.bouncr.api.boundary.PasswordCredentialUpdateRequest;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.OtpKey;
import net.unit8.bouncr.entity.PasswordCredential;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.Arrays;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.DELETE;
import static kotowari.restful.DecisionPoint.POST;
import static kotowari.restful.DecisionPoint.PUT;

public class PasswordCredentialResource {
    @Inject
    private BouncrConfiguration config;

    @Decision(POST)
    public PasswordCredential create(PasswordCredentialUpdateRequest createRequest, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(cb.equal(userRoot.get("name"), principal.getName()));
        User user = em.createQuery(query).getResultStream().findAny().orElseThrow();
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        PasswordCredential passwordCredential = builder(new PasswordCredential())
                .set(PasswordCredential::setUser, user)
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(createRequest.getNewPassword(), salt, 100))
                .build();

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(passwordCredential));
        em.detach(passwordCredential);
        return passwordCredential;
    }

    @Decision(PUT)
    public PasswordCredential update(PasswordCredentialUpdateRequest updateRequest, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PasswordCredential> passwordCredentialQuery = cb.createQuery(PasswordCredential.class);
        Root<PasswordCredential> passwordCredentialRoot = passwordCredentialQuery.from(PasswordCredential.class);
        Join<User, PasswordCredential> userJoin = passwordCredentialRoot.join("user");

        passwordCredentialQuery.where(cb.equal(userJoin.get("name"), principal.getName()));
        PasswordCredential passwordCredential = em.createQuery(passwordCredentialQuery)
                .getResultStream()
                .findAny()
                .orElseThrow();

        Arrays.equals(passwordCredential.getPassword(),
                PasswordUtils.pbkdf2(updateRequest.getOldPassword(), passwordCredential.getSalt(), 100));
        passwordCredential.setCreatedAt(LocalDateTime.now());


        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.merge(passwordCredential));
        em.detach(passwordCredential);
        return passwordCredential;
    }

    @Decision(DELETE)
    public Void delete(UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OtpKey> query = cb.createQuery(OtpKey.class);
        Root<OtpKey> otpKeyRoot = query.from(OtpKey.class);
        Join<User, OtpKey> userRoot = otpKeyRoot.join("user");
        query.where(cb.equal(userRoot.get("name"), principal.getName()));
        EntityTransactionManager tx = new EntityTransactionManager(em);
        em.createQuery(query)
                .getResultStream()
                .findAny()
                .ifPresent(otpKey -> tx.required(() -> em.remove(otpKey)));
        return null;
    }

}
