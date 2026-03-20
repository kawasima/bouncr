package net.unit8.bouncr.extention.app.resource;

import enkan.Env;
import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfileField;
import net.unit8.bouncr.data.UserProfileValue;
import net.unit8.bouncr.data.UserProfileVerification;
import net.unit8.bouncr.data.UserProfileVerificationId;
import net.unit8.bouncr.hook.email.config.MailConfig;
import net.unit8.bouncr.hook.email.service.SendMailService;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static org.jooq.impl.DSL.*;

@AllowedMethods({"PUT"})
public class VerificationEmailResource {
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<UserProfileValue> EMAIL_VALUE = ContextKey.of(UserProfileValue.class);
    static final ContextKey<UserProfileVerification> VERIFICATION = ContextKey.of(UserProfileVerification.class);

    @Inject
    private MailConfig mailConfig;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean isAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read")
                        || p.hasPermission("any_user:read")
                        || (p.hasPermission("my:read") && Objects.equals(p.getName(), params.get("account"))))
                .isPresent();
    }

    private Optional<UserProfileVerification> findMailVerification(DSLContext dsl, String account) {
        return dsl.select(
                        field("upv.user_profile_field_id", Long.class).as("user_profile_field_id"),
                        field("upv.user_id", Long.class).as("user_id"),
                        field("upv.code", String.class).as("code"),
                        field("upv.expires_at", LocalDateTime.class).as("expires_at"))
                .from(table("user_profile_verifications").as("upv"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("upv.user_id")))
                .join(table("user_profile_fields").as("upf")).on(field("upf.user_profile_field_id").eq(field("upv.user_profile_field_id")))
                .where(field("u.account").eq(account)
                        .and(field("upf.json_name").eq("email")))
                .fetchOptional(rec -> new UserProfileVerification(
                        new UserProfileVerificationId(
                                rec.get(field("user_profile_field_id", Long.class)),
                                rec.get(field("user_id", Long.class))),
                        rec.get(field("code", String.class)),
                        rec.get(field("expires_at", LocalDateTime.class))));
    }

    private User loadUserWithProfileValues(DSLContext dsl, String account) {
        var userRec = dsl.select(
                        field("user_id", Long.class),
                        field("account", String.class),
                        field("write_protected", Boolean.class))
                .from(table("users"))
                .where(field("account").eq(account))
                .fetchOne();
        if (userRec == null) return null;

        Long userId = userRec.get(field("user_id", Long.class));
        String userAccount = userRec.get(field("account", String.class));
        Boolean writeProtected = userRec.get(field("write_protected", Boolean.class));

        List<UserProfileValue> profileValues = dsl.select(
                        field("upf.user_profile_field_id", Long.class).as("user_profile_field_id"),
                        field("upf.name", String.class).as("name"),
                        field("upf.json_name", String.class).as("json_name"),
                        field("upf.is_required", Boolean.class).as("is_required"),
                        field("upf.is_identity", Boolean.class).as("is_identity"),
                        field("upf.regular_expression", String.class).as("regular_expression"),
                        field("upf.max_length", Integer.class).as("max_length"),
                        field("upf.min_length", Integer.class).as("min_length"),
                        field("upf.needs_verification", Boolean.class).as("needs_verification"),
                        field("upf.position", Integer.class).as("position"),
                        field("upv.\"value\"", String.class).as("value"))
                .from(table("user_profile_values").as("upv"))
                .join(table("user_profile_fields").as("upf")).on(field("upf.user_profile_field_id").eq(field("upv.user_profile_field_id")))
                .where(field("upv.user_id").eq(userId))
                .fetch(rec -> {
                    UserProfileField upf = new UserProfileField(
                            rec.get(field("user_profile_field_id", Long.class)),
                            rec.get(field("name", String.class)),
                            rec.get(field("json_name", String.class)),
                            rec.get(field("is_required", Boolean.class)),
                            rec.get(field("is_identity", Boolean.class)),
                            rec.get(field("regular_expression", String.class)),
                            rec.get(field("max_length", Integer.class)),
                            rec.get(field("min_length", Integer.class)),
                            rec.get(field("needs_verification", Boolean.class)),
                            rec.get(field("position", Integer.class)));
                    return new UserProfileValue(upf, null, rec.get(field("value", String.class)));
                });

        return new User(userId, userAccount, writeProtected,
                null, profileValues, null, null, null, null, null, null);
    }

    @Decision(PROCESSABLE)
    public boolean processable(Parameters params, DSLContext dsl, RestContext context) {
        User user = loadUserWithProfileValues(dsl, params.get("account"));

        if (user != null) {
            context.put(USER, user);
        } else {
            context.setMessage(Problem.valueOf(422, "User not found"));
            return false;
        }

        UserProfileValue emailValue = findMailVerification(dsl, user.account()).map(verification -> {
            context.put(VERIFICATION, verification);
            return verification;
        }).flatMap(verification -> user.userProfileValues()
                .stream()
                .filter(v -> Objects.equals(v.userProfileField().jsonName(), "email"))
                .findAny()
                .filter(v -> Objects.nonNull(v.value()))
        ).orElse(null);

        if (emailValue != null) {
            context.put(EMAIL_VALUE, emailValue);
        }
        return emailValue != null;
    }

    @Decision(PUT)
    public Void update(User user, UserProfileValue emailValue, UserProfileVerification emailVerification) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("baseUrl", Env.getString("EMAIL_BASE_URL", "http://localhost:3000/bouncr/api"));
        variables.put("user", user);
        variables.put("email", emailValue.value());
        variables.put("code", emailVerification.code());

        final SendMailService sendMailService = new SendMailService(mailConfig);
        sendMailService.send(emailValue.value(), "Verification", variables);
        return null;
    }

}
