package net.unit8.bouncr.api.service;

import enkan.data.HttpRequest;
import enkan.util.jpa.EntityTransactionManager;
import net.unit8.bouncr.api.authn.OneTimePasswordGenerator;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.*;

import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.*;
import static net.unit8.bouncr.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.entity.ActionType.USER_SIGNIN;

public class SignInService {
    private final BouncrConfiguration config;
    private final EntityManager em;

    public SignInService(EntityManager em, BouncrConfiguration config) {
        this.config = config;
        this.em = em;
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
            userActionCriteria.where(userActionRoot.get("actionType").in(USER_SIGNIN, USER_FAILED_SIGNIN));

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

    public boolean validateOtpKey(OtpKey otpKey, String code) {
        if (otpKey == null) return true;

        return new OneTimePasswordGenerator(30)
                .generateTotpSet(otpKey.getKey(), 5)
                .stream()
                .map(n -> String.format(Locale.US, "%06d", n))
                .collect(Collectors.toSet())
                .contains(code);
    }

    public PasswordCredentialStatus validatePasswordCredentialAttributes(User user) {
        PasswordCredential passwordCredential = user.getPasswordCredential();
        if (passwordCredential.isInitial()) {
            return INITIAL;
        }

        if (config.getPasswordPolicy().getExpires() != null) {
            Instant createdAt = passwordCredential.getCreatedAt().toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
            return (createdAt.plus(config.getPasswordPolicy().getExpires()).isBefore(config.getClock().instant())) ?
                    EXPIRED : VALID;
        }

        return VALID;
    }

    public enum PasswordCredentialStatus {
        VALID,
        INITIAL,
        EXPIRED
    }
}
