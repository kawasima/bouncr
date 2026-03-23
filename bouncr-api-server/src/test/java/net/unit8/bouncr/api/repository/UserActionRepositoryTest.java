package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.api.resource.MockFactory;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.UserAction;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserActionRepositoryTest {
    private DSLContext dsl;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void insertAndSearch() {
        UserActionRepository repo = new UserActionRepository(dsl);
        repo.insert(ActionType.USER_SIGNIN.getName(), "admin", "127.0.0.1", null, LocalDateTime.now());

        List<UserAction> actions = repo.search("admin", 0, 10);
        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().actor()).isEqualTo("admin");
        assertThat(actions.getFirst().actionType()).isEqualTo(ActionType.USER_SIGNIN);
    }
}
