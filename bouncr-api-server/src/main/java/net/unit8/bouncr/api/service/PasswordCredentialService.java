package net.unit8.bouncr.api.service;

import net.unit8.bouncr.data.InitialPassword;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.PasswordCredential;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;

import javax.persistence.EntityManager;

import java.time.LocalDateTime;

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

    public InitialPassword initializePassword(User user) {
        PasswordCredential passwordCredential = user.getPasswordCredential();
        if (passwordCredential != null) {
            em.remove(passwordCredential);
            em.flush();
        }
        String password = RandomUtils.generateRandomString(8, config.getSecureRandom());
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredential = builder(new PasswordCredential())
                .set(PasswordCredential::setUser, user)
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(password, salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, true)
                .set(PasswordCredential::setCreatedAt, LocalDateTime.now())
                .build();
        em.persist(passwordCredential);
        user.setPasswordCredential(passwordCredential);
        return builder(new InitialPassword())
                .set(InitialPassword::setPassword, password)
                .build();

    }
}
