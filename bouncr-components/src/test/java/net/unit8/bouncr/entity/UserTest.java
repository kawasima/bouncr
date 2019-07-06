package net.unit8.bouncr.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.persistence.indirection.IndirectList;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserTest {
    @Test
    void cyclicSerialize() {
        ObjectMapper mapper = new ObjectMapper();
        User user = builder(new User())
                .set(User::setId, 1L)
                .set(User::setAccount, "test")
                .set(User::setGroups, new IndirectList<>())
                .build();
        UserLock userLock = builder(new UserLock())
                .set(UserLock::setUser, user)
                .set(UserLock::setLockedAt, LocalDateTime.now())
                .set(UserLock::setLockLevel, LockLevel.LOOSE)
                .build();
        user.setUserLock(userLock);

        assertThatCode(() -> {
            mapper.writerFor(User.class).writeValueAsString(user);
            mapper.writerFor(UserLock.class).writeValueAsString(userLock);
        }).doesNotThrowAnyException();
    }
}