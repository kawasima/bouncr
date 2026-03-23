package net.unit8.bouncr.entity;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import net.unit8.bouncr.data.LockLevel;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserLock;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class UserTest {
    @Test
    void cyclicSerialize() {
        JsonMapper mapper = JsonMapper.builder().build();
        User user = new User(1L, "test", false,
                List.of(), null, null, null, null, null);
        UserLock userLock = new UserLock(LockLevel.LOOSE, LocalDateTime.now());

        assertThatCode(() -> {
            mapper.writerFor(User.class).writeValueAsString(user);
            mapper.writerFor(UserLock.class).writeValueAsString(userLock);
        }).doesNotThrowAnyException();
    }
}
