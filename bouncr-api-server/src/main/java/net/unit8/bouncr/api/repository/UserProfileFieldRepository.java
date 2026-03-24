package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.UserProfileField;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.USER_PROFILE_FIELD;
import static org.jooq.impl.DSL.*;

public class UserProfileFieldRepository {
    private final DSLContext dsl;

    public UserProfileFieldRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<UserProfileField> findAll() {
        return dsl.select(
                        field("user_profile_field_id", Long.class),
                        field("name", String.class),
                        field("json_name", String.class),
                        field("is_required", Boolean.class),
                        field("is_identity", Boolean.class),
                        field("regular_expression", String.class),
                        field("max_length", Integer.class),
                        field("min_length", Integer.class),
                        field("needs_verification", Boolean.class),
                        field("position", Integer.class))
                .from(table("user_profile_fields"))
                .orderBy(field("position").asc())
                .fetch(rec -> USER_PROFILE_FIELD.decode(rec).getOrThrow());
    }

    public Optional<UserProfileField> findByJsonName(String jsonName) {
        var rec = dsl.select(
                        field("user_profile_field_id", Long.class),
                        field("name", String.class),
                        field("json_name", String.class),
                        field("is_required", Boolean.class),
                        field("is_identity", Boolean.class),
                        field("regular_expression", String.class),
                        field("max_length", Integer.class),
                        field("min_length", Integer.class),
                        field("needs_verification", Boolean.class),
                        field("position", Integer.class))
                .from(table("user_profile_fields"))
                .where(field("json_name").eq(jsonName))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(USER_PROFILE_FIELD.decode(rec).getOrThrow());
    }

    public List<UserProfileField> findIdentityFields() {
        return dsl.select(
                        field("user_profile_field_id", Long.class),
                        field("name", String.class),
                        field("json_name", String.class),
                        field("is_required", Boolean.class),
                        field("is_identity", Boolean.class),
                        field("regular_expression", String.class),
                        field("max_length", Integer.class),
                        field("min_length", Integer.class),
                        field("needs_verification", Boolean.class),
                        field("position", Integer.class))
                .from(table("user_profile_fields"))
                .where(field("is_identity").eq(true))
                .orderBy(field("position").asc())
                .fetch(rec -> USER_PROFILE_FIELD.decode(rec).getOrThrow());
    }
}
