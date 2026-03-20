package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.UserProfileVerification;
import net.unit8.bouncr.data.UserProfileVerificationId;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

public class UserProfileVerificationRepository {
    private final DSLContext dsl;

    public UserProfileVerificationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UserProfileVerification> findByCode(String code) {
        var rec = dsl.select(
                        field("user_profile_field_id", Long.class),
                        field("user_id", Long.class),
                        field("code", String.class),
                        field("expires_at", LocalDateTime.class))
                .from(table("user_profile_verifications"))
                .where(field("code").eq(code))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(new UserProfileVerification(
                new UserProfileVerificationId(
                        rec.get(field("user_profile_field_id", Long.class)),
                        rec.get(field("user_id", Long.class))),
                rec.get(field("code", String.class)),
                rec.get(field("expires_at", LocalDateTime.class))));
    }

    public void delete(Long userProfileFieldId, Long userId) {
        dsl.deleteFrom(table("user_profile_verifications"))
                .where(field("user_profile_field_id").eq(userProfileFieldId)
                        .and(field("user_id").eq(userId)))
                .execute();
    }
}
