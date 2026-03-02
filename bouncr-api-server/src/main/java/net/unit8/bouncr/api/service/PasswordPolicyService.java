package net.unit8.bouncr.api.service;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.api.boundary.PasswordCredentialCreateRequest;
import net.unit8.bouncr.api.boundary.PasswordCredentialUpdateRequest;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.entity.PasswordCredential;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.PasswordUtils;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static enkan.util.ThreadingUtils.some;

public class PasswordPolicyService {
    private EntityManager em;
    private PasswordPolicy policy;

    public PasswordPolicyService(PasswordPolicy policy, EntityManager em) {
        this.em = em;
        this.policy = policy;
    }

    protected Problem.Violation conformPolicy(String password) {
        int passwordLen = some(password, String::length).orElse(0);
        if (passwordLen > policy.getMaxLength()) {
            return new Problem.Violation("passwrod", "must be less than " + policy.getMaxLength() + " characters");
        }

        if (passwordLen < policy.getMinLength()) {
            return new Problem.Violation("passwrod", "must be greater than " + policy.getMinLength() + " characters");
        }

        return Optional.ofNullable(policy.getPattern())
                .filter(ptn -> !ptn.matcher(password).matches())
                .map(ptn -> new Problem.Violation("password", "doesn't match pattern"))
                .orElse(null);
    }

    public Problem.Violation validateCreatePassword(PasswordCredentialCreateRequest createRequest) {
        return conformPolicy(createRequest.getPassword());
    }

    public Problem.Violation validateUpdatePassword(PasswordCredentialUpdateRequest updateRequest) {
        if (Objects.equals(updateRequest.getNewPassword(), updateRequest.getOldPassword())) {
            return new Problem.Violation("new_password", "is the same as the old password");
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PasswordCredential> query = cb.createQuery(PasswordCredential.class);
        Root<PasswordCredential> passwordCredentialRoot = query.from(PasswordCredential.class);
        Join<User, PasswordCredential> userJoin = passwordCredentialRoot.join("user");
        query.where(cb.equal(userJoin.get("account"), updateRequest.getAccount()));
        PasswordCredential passwordCredential = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (passwordCredential == null) {
            return new Problem.Violation("old_password", "does not match current password");
        }

        byte[] currentPassword = passwordCredential.getPassword();
        byte[] oldPassword = PasswordUtils.pbkdf2(updateRequest.getOldPassword(), passwordCredential.getSalt(), 600_000);
        if (!Arrays.equals(currentPassword, oldPassword)) {
            return new Problem.Violation("old_password", "does not match current password");
        }
        return conformPolicy(updateRequest.getNewPassword());
    }
}
