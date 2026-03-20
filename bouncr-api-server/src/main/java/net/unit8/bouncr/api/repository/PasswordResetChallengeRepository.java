package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.PasswordResetChallenge;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class PasswordResetChallengeRepository {
    private final DSLContext dsl;

    public PasswordResetChallengeRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<PasswordResetChallenge> findByCode(String code) {
        var rec = dsl.select(
                        field("prc.id", Long.class).as("id"),
                        field("prc.user_id", Long.class).as("user_id"),
                        field("prc.code", String.class).as("code"),
                        field("prc.expires_at", LocalDateTime.class).as("expires_at"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("password_reset_challenges").as("prc"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("prc.user_id")))
                .where(field("prc.code").eq(code))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(PRC_WITH_USER.decode(rec).getOrThrow());
    }

    public Optional<PasswordResetChallenge> findActiveByCode(String code) {
        var rec = dsl.select(
                        field("prc.id", Long.class).as("id"),
                        field("prc.user_id", Long.class).as("user_id"),
                        field("prc.code", String.class).as("code"),
                        field("prc.expires_at", LocalDateTime.class).as("expires_at"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("password_reset_challenges").as("prc"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("prc.user_id")))
                .where(field("prc.code").eq(code)
                        .and(field("prc.expires_at", LocalDateTime.class).greaterThan(LocalDateTime.now())))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(PRC_WITH_USER.decode(rec).getOrThrow());
    }

    public PasswordResetChallenge insert(Long userId, String code, LocalDateTime expiresAt) {
        Record rec = dsl.insertInto(table("password_reset_challenges"),
                        field("user_id"), field("code"), field("expires_at"))
                .values(userId, code, expiresAt)
                .returningResult(
                        field("id", Long.class),
                        field("user_id", Long.class),
                        field("code", String.class),
                        field("expires_at", LocalDateTime.class))
                .fetchOne();

        return PASSWORD_RESET_CHALLENGE.decode(rec).getOrThrow();
    }

    public void delete(Long id) {
        dsl.deleteFrom(table("password_reset_challenges"))
                .where(field("id").eq(id))
                .execute();
    }
}
