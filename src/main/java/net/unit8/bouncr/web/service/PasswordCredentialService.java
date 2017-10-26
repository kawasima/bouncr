package net.unit8.bouncr.web.service;

import enkan.component.doma2.DomaProvider;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.PasswordCredentialDao;
import net.unit8.bouncr.web.entity.PasswordCredential;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.FormBase;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Optional;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ReflectionUtils.tryReflection;
import static enkan.util.ThreadingUtils.some;

public class PasswordCredentialService {
    private final DomaProvider daoProvider;
    private final BouncrConfiguration config;

    public PasswordCredentialService(DomaProvider daoProvider, BouncrConfiguration config) {
        this.daoProvider = daoProvider;
        this.config = config;
    }

    public void changePassword(User user, String password) {
        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);

        passwordCredentialDao.deleteById(user.getId());
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredentialDao.insert(builder(new PasswordCredential())
                .set(PasswordCredential::setId, user.getId())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(password, salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, false)
                .build());
    }

    public void validateBasedOnPasswordPolicy(FormBase form, String passwordPropertyName) {
        String password = tryReflection(() -> {
            Field f = form.getClass().getDeclaredField(passwordPropertyName);
            f.setAccessible(true);
            return (String) f.get(form);
        });
        int passwordLen = some(password, p -> p.length()).orElse(0);
        int maxLen = config.getPasswordPolicy().getMaxLength();
        int minLen = config.getPasswordPolicy().getMinLength();
        if (passwordLen > maxLen) {
            MessageFormat fmt = new MessageFormat("length must be {0} and {1}");
            form.getErrors().put(passwordPropertyName, fmt.format(new Object[]{minLen, maxLen}));
        }

        if (passwordLen < config.getPasswordPolicy().getMinLength()) {
            MessageFormat fmt = new MessageFormat("length must be {0} and {1}");
            form.getErrors().put(passwordPropertyName, fmt.format(new Object[]{minLen, maxLen}));
        }

        Optional.ofNullable(config.getPasswordPolicy().getPattern()).ifPresent(p -> {
            if (!p.matcher(password).matches()) {
                MessageFormat fmt = new MessageFormat("contains unavailable characters");
                form.getErrors().put(passwordPropertyName, fmt.format(new Object[]{}));
            }
        });
    }
}
