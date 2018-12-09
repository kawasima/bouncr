package net.unit8.bouncr.api.service;

import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.PasswordCredential;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;

import javax.persistence.EntityManager;

import static enkan.util.BeanBuilder.builder;

public class PasswordCredentialService {
    private final EntityManager em;
    private final BouncrConfiguration config;

    public PasswordCredentialService(EntityManager em, BouncrConfiguration config) {
        this.em = em;
        this.config = config;
    }

    public void changePassword(User user, String password) {
        PasswordCredential passwordCredential = em.find(PasswordCredential.class, user);
        em.remove(passwordCredential);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredential = builder(new PasswordCredential())
                .set(PasswordCredential::setUser, user)
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(password, salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, false)
                .build();
        em.persist(passwordCredential);
    }

    /*
    public ConstraintViolation<> validateBasedOnPasswordPolicy(String password) {
        int passwordLen = some(password, p -> p.length()).orElse(0);
        int maxLen = config.getPasswordPolicy().getMaxLength();
        int minLen = config.getPasswordPolicy().getMinLength();
        MessageResource messageResource = config.getMessageResource();
        if (passwordLen > maxLen) {
            form.getErrors().put(passwordPropertyName,
                    messageResource.renderMessage(Locale.getDefault(), "error.passwordLength", minLen, maxLen));
        }

        if (passwordLen < minLen) {
            form.getErrors().put(passwordPropertyName,
                    messageResource.renderMessage(Locale.getDefault(), "error.passwordLength", minLen, maxLen));
        }

        Optional.ofNullable(config.getPasswordPolicy().getPattern()).ifPresent(p -> {
            if (!p.matcher(password).matches()) {
                form.getErrors().put(passwordPropertyName,
                        messageResource.renderMessage(Locale.getDefault(), "error.mismatchPasswordPattern"));
            }
        });
    }
    */

}
