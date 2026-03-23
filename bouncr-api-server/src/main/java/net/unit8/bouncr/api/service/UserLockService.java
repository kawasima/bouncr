package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.LockLevel;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserCredentials;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static net.unit8.bouncr.data.ActionType.*;
import static org.jooq.impl.DSL.*;

public class UserLockService {
    private final BouncrConfiguration config;
    private final DSLContext dsl;

    public UserLockService(DSLContext dsl, BouncrConfiguration config) {
        this.dsl = dsl;
        this.config = config;
    }

    public void lockUser(UserCredentials creds) {
        if (creds == null) return;

        int numTrials = config.getPasswordPolicy().getNumOfTrialsUntilLock();
        List<String> recentActionNames = dsl.select(field("a.name", String.class).as("name"))
                .from(table("user_actions").as("ua"))
                .join(table("actions").as("a")).on(field("a.action_id").eq(field("ua.action_id")))
                .where(field("ua.actor").eq(creds.account()))
                .and(field("a.name").in(USER_SIGNIN.name(), USER_FAILED_SIGNIN.name(), PASSWORD_CHANGED.name()))
                .orderBy(field("ua.created_at").desc())
                .limit(numTrials)
                .fetch(rec -> rec.get(field("name", String.class)));

        if (recentActionNames.size() == numTrials
                && recentActionNames.stream().allMatch(name -> name.equals(USER_FAILED_SIGNIN.name()))
                && creds.userLock() == null) {
            UserRepository repo = new UserRepository(dsl);
            repo.lockUser(creds.id(), LockLevel.LOOSE);
        }
    }

    public void unlockUser(User user) {
        if (user.userLock() != null && user.userLock().lockLevel() == LockLevel.LOOSE) {
            UserRepository repo = new UserRepository(dsl);
            repo.unlockUser(user.id());
        }
    }
}
