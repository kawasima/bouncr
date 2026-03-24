package net.unit8.bouncr.api.service;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.raoh.combinator.Tuple3;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.jooq.impl.DSL.*;

public class PasswordPolicyService {
    private final DSLContext dsl;
    private final PasswordPolicy policy;
    private final int pbkdf2Iterations;

    public PasswordPolicyService(PasswordPolicy policy, DSLContext dsl, int pbkdf2Iterations) {
        this.dsl = dsl;
        this.policy = policy;
        this.pbkdf2Iterations = pbkdf2Iterations;
    }

    public Problem.Violation conformPolicy(String password) {
        if (policy.getMinLength() > 0 && password.length() < policy.getMinLength()) {
            return new Problem.Violation("password", "must be at least " + policy.getMinLength() + " characters");
        }
        return null;
    }

    /**
     * Verifies a password change request.
     *
     * @param updateRequest Tuple3(account, oldPassword, newPassword)
     */
    public Problem.Violation verifyPasswordChange(Tuple3<String, String, String> updateRequest) {
        var rec = dsl.select(
                        field("pc.password", byte[].class),
                        field("pc.salt", String.class),
                        field("pc.created_at", LocalDateTime.class))
                .from(table("password_credentials").as("pc"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("pc.user_id")))
                .where(field("u.account").eq(updateRequest._1()))
                .fetchOne();

        if (rec == null) {
            return new Problem.Violation("old_password", "does not match current password");
        }

        byte[] currentPassword = rec.get(field("password", byte[].class));
        byte[] oldPassword = PasswordUtils.pbkdf2(updateRequest._2(), rec.get(field("salt", String.class)), pbkdf2Iterations);
        if (!Arrays.equals(currentPassword, oldPassword)) {
            return new Problem.Violation("old_password", "does not match current password");
        }
        return conformPolicy(updateRequest._3());
    }
}
