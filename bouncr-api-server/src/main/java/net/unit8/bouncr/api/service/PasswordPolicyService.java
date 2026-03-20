package net.unit8.bouncr.api.service;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.util.PasswordUtils;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static enkan.util.ThreadingUtils.some;
import static org.jooq.impl.DSL.*;

public class PasswordPolicyService {
    private final DSLContext dsl;
    private final PasswordPolicy policy;

    public PasswordPolicyService(PasswordPolicy policy, DSLContext dsl) {
        this.dsl = dsl;
        this.policy = policy;
    }

    protected Problem.Violation conformPolicy(String password) {
        int passwordLen = some(password, String::length).orElse(0);
        if (passwordLen > policy.getMaxLength()) {
            return new Problem.Violation("password", "must be less than " + policy.getMaxLength() + " characters");
        }

        if (passwordLen < policy.getMinLength()) {
            return new Problem.Violation("password", "must be greater than " + policy.getMinLength() + " characters");
        }

        return Optional.ofNullable(policy.getPattern())
                .filter(ptn -> !ptn.matcher(password).matches())
                .map(ptn -> new Problem.Violation("password", "doesn't match pattern"))
                .orElse(null);
    }

    public Problem.Violation validateCreatePassword(BouncrJsonDecoders.PasswordCredentialCreate createRequest) {
        return conformPolicy(createRequest.password());
    }

    public Problem.Violation validateUpdatePassword(BouncrJsonDecoders.PasswordCredentialUpdate updateRequest) {
        if (Objects.equals(updateRequest.newPassword(), updateRequest.oldPassword())) {
            return new Problem.Violation("new_password", "is the same as the old password");
        }

        var rec = dsl.select(
                        field("pc.password", byte[].class).as("password"),
                        field("pc.salt", String.class).as("salt"))
                .from(table("password_credentials").as("pc"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("pc.user_id")))
                .where(field("u.account").eq(updateRequest.account()))
                .fetchOne();

        if (rec == null) {
            return new Problem.Violation("old_password", "does not match current password");
        }

        byte[] currentPassword = rec.get(field("password", byte[].class));
        byte[] oldPassword = PasswordUtils.pbkdf2(updateRequest.oldPassword(), rec.get(field("salt", String.class)), 600_000);
        if (!Arrays.equals(currentPassword, oldPassword)) {
            return new Problem.Violation("old_password", "does not match current password");
        }
        return conformPolicy(updateRequest.newPassword());
    }
}
