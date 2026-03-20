package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.UserSession;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class UserSessionRepository {
    private final DSLContext dsl;

    public UserSessionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UserSession> findByToken(String token) {
        var rec = dsl.select(
                        field("s.user_session_id", Long.class).as("user_session_id"),
                        field("s.user_id", Long.class).as("user_id"),
                        field("s.token", String.class).as("token"),
                        field("s.remote_address", String.class).as("remote_address"),
                        field("s.user_agent", String.class).as("user_agent"),
                        field("s.created_at", LocalDateTime.class).as("created_at"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("user_sessions").as("s"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("s.user_id")))
                .where(field("s.token").eq(token))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(USER_SESSION_WITH_USER.decode(rec).getOrThrow());
    }

    public List<UserSession> searchByUserId(Long userId, int offset, int limit) {
        return dsl.select(
                        field("s.user_session_id", Long.class).as("user_session_id"),
                        field("s.user_id", Long.class).as("user_id"),
                        field("s.token", String.class).as("token"),
                        field("s.remote_address", String.class).as("remote_address"),
                        field("s.user_agent", String.class).as("user_agent"),
                        field("s.created_at", LocalDateTime.class).as("created_at"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("user_sessions").as("s"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("s.user_id")))
                .where(field("s.user_id").eq(userId))
                .orderBy(field("s.user_session_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> USER_SESSION_WITH_USER.decode(rec).getOrThrow());
    }

    public UserSession insert(Long userId, String token, String remoteAddress, String userAgent, LocalDateTime createdAt) {
        Record rec = dsl.insertInto(table("user_sessions"),
                        field("user_id"), field("token"), field("remote_address"),
                        field("user_agent"), field("created_at"))
                .values(userId, token, remoteAddress, userAgent, createdAt)
                .returningResult(
                        field("user_session_id", Long.class),
                        field("user_id", Long.class),
                        field("token", String.class),
                        field("remote_address", String.class),
                        field("user_agent", String.class),
                        field("created_at", LocalDateTime.class))
                .fetchOne();

        return USER_SESSION.decode(rec).getOrThrow();
    }

    public void deleteByToken(String token) {
        dsl.deleteFrom(table("user_sessions"))
                .where(field("token").eq(token))
                .execute();
    }
}
