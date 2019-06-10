package net.unit8.bouncr.api.service;

import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.LockLevel;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserAction;
import net.unit8.bouncr.entity.UserLock;

import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.List;

import static enkan.util.BeanBuilder.builder;
import static net.unit8.bouncr.entity.ActionType.*;

public class UserLockService {
    private final BouncrConfiguration config;
    private final EntityManager em;

    public UserLockService(EntityManager em, BouncrConfiguration config) {
        this.em = em;
        this.config = config;
    }

    /**
     * Record the event of signing in
     *
     * @param user    the user entity
     */
    public void lockUser(User user) {
        if (user != null) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserAction> userActionCriteria = cb.createQuery(UserAction.class);
            Root<UserAction> userActionRoot = userActionCriteria.from(UserAction.class);
            userActionCriteria.where(userActionRoot.get("actionType").in(USER_SIGNIN, USER_FAILED_SIGNIN, PASSWORD_CHANGED));
            userActionCriteria.orderBy(cb.desc(userActionRoot.get("createdAt")));

            List<UserAction> recentActions = em.createQuery(userActionCriteria)
                    .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                    .setMaxResults(config.getPasswordPolicy().getNumOfTrialsUntilLock())
                    .getResultList();
            if (recentActions.size() == config.getPasswordPolicy().getNumOfTrialsUntilLock() &&
                    recentActions.stream()
                            .allMatch(action -> action.getActionType() == USER_FAILED_SIGNIN) &&
                    user.getUserLock() == null
            ) {
                em.persist(builder(new UserLock())
                        .set(UserLock::setUser, user)
                        .set(UserLock::setLockLevel, LockLevel.LOOSE)
                        .set(UserLock::setLockedAt, LocalDateTime.now())
                        .build());
            }
        }
    }

    public void unlockUser(User user) {
        UserLock userLock = user.getUserLock();
        if (userLock != null && userLock.getLockLevel() == LockLevel.LOOSE) {
            em.remove(userLock);
        }
    }


}
