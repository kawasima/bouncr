package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.InitialPassword;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import org.jooq.DSLContext;

public class PasswordCredentialService {
    private final DSLContext dsl;
    private final BouncrConfiguration config;

    public PasswordCredentialService(DSLContext dsl, BouncrConfiguration config) {
        this.dsl = dsl;
        this.config = config;
    }

    public void changePassword(User user, String password) {
        UserRepository repo = new UserRepository(dsl);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        byte[] hash = PasswordUtils.pbkdf2(password, salt, 600_000);
        repo.deletePasswordCredential(user.id());
        repo.insertPasswordCredential(user.id(), hash, salt, false);
    }

    public InitialPassword initializePassword(User user) {
        String password = RandomUtils.generateRandomString(8, config.getSecureRandom());
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        byte[] hash = PasswordUtils.pbkdf2(password, salt, 600_000);

        UserRepository repo = new UserRepository(dsl);
        repo.insertPasswordCredential(user.id(), hash, salt, true);

        return new InitialPassword(password);
    }
}
