package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.entity.UserProfileVerification;

import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static kotowari.restful.DecisionPoint.DELETE;
import static kotowari.restful.DecisionPoint.EXISTS;

@AllowedMethods("DELETE")
public class UserProfileVerificationResource {
    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserProfileVerification> query = cb.createQuery(UserProfileVerification.class);
        Root<UserProfileVerification> userProfileVerificationRoot = query.from(UserProfileVerification.class);
        query.where(cb.equal(userProfileVerificationRoot.get("code"), params.get("code")));

        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream()
                .findAny()
                .map(verification -> {
                    context.putValue(verification);
                    return verification;
                })
                .isPresent();
    }

    @Decision(DELETE)
    public Void delete(UserProfileVerification userProfileVerification, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(userProfileVerification));
        em.detach(userProfileVerification);
        return null;
    }
}
