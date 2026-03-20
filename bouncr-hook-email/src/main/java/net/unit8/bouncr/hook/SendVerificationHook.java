package net.unit8.bouncr.hook;

import enkan.Env;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfileValue;
import net.unit8.bouncr.data.UserProfileVerification;
import net.unit8.bouncr.data.VerificationTargetSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SendVerificationHook extends AbstractSendMailHook {
    public SendVerificationHook() {
    }

    protected Map<String, Object> createContext(RestContext context) {
        Map<String, Object> variables = new HashMap<>();

        context.getByType(User.class).ifPresent(user -> {
            variables.put("baseUrl", Env.getString("EMAIL_BASE_URL", "http://localhost:3000/bouncr/api"));
            variables.put("user", user);
            variables.put("email", user.userProfileValues().stream()
                    .filter(v -> Objects.equals(v.userProfileField().jsonName(), "email"))
                    .map(UserProfileValue::value)
                    .findAny()
                    .orElse("Unknown"));
        });

        context.getByType(VerificationTargetSet.class).ifPresent(verifications -> {
            // Find the email field ID from the user's profile values, then match against verifications
            context.getByType(User.class).ifPresent(user -> {
                user.userProfileValues().stream()
                        .filter(v -> Objects.equals(v.userProfileField().jsonName(), "email"))
                        .findAny()
                        .ifPresent(emailValue -> {
                            Long emailFieldId = emailValue.userProfileField().id();
                            verifications.stream()
                                    .filter(v -> Objects.equals(v.id().userProfileField(), emailFieldId))
                                    .findAny()
                                    .ifPresent(verification -> variables.put("code", verification.code()));
                        });
            });
        });
        return variables;
    }

    @Override
    protected Optional<UserProfileValue> findEmailField(RestContext context) {
        return context.getByType(VerificationTargetSet.class).flatMap(verificationTargetSet ->
                context.getByType(User.class).flatMap(user -> {
                    // Find the email profile value
                    Optional<UserProfileValue> emailValue = user.userProfileValues().stream()
                            .filter(v -> Objects.equals(v.userProfileField().jsonName(), "email"))
                            .findAny();
                    // Only return it if there's a matching verification for the email field
                    return emailValue.filter(ev -> {
                        Long emailFieldId = ev.userProfileField().id();
                        return verificationTargetSet.stream()
                                .anyMatch(v -> Objects.equals(v.id().userProfileField(), emailFieldId));
                    });
                }));
    }

    @Override
    public String getMetaKey() {
        return "Verification";
    }
}
