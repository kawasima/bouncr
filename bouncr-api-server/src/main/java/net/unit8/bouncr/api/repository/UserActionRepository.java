package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.UserAction;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class UserActionRepository {
    private final DSLContext dsl;

    public UserActionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<UserAction> search(String account, int offset, int limit) {
        var condition = noCondition();
        if (account != null && !account.isEmpty()) {
            condition = condition.and(field("ua.actor").eq(account));
        }

        return dsl.select(
                        field("ua.user_action_id", Long.class).as("user_action_id"),
                        field("ua.action_id", Long.class).as("action_id"),
                        field("ua.actor", String.class).as("actor"),
                        field("ua.actor_ip", String.class).as("actor_ip"),
                        field("ua.options", String.class).as("options"),
                        field("ua.created_at", LocalDateTime.class).as("created_at"))
                .from(table("user_actions").as("ua"))
                .where(condition)
                .orderBy(field("ua.user_action_id").desc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> USER_ACTION.decode(rec).getOrThrow());
    }

    public void insert(String actionName, String actor, String actorIp, String options, LocalDateTime createdAt) {
        Long actionId = dsl.select(field("action_id", Long.class))
                .from(table("actions"))
                .where(field("name").eq(actionName))
                .fetchOne(field("action_id", Long.class));

        if (actionId == null) return;

        dsl.insertInto(table("user_actions"),
                        field("action_id"), field("actor"), field("actor_ip"),
                        field("options"), field("created_at"))
                .values(actionId, actor, actorIp, options, createdAt)
                .execute();
    }
}
