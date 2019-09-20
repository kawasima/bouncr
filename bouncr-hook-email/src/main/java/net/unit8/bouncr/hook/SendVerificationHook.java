package net.unit8.bouncr.hook;

import enkan.Env;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.domain.VerificationTargetSet;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileField;
import net.unit8.bouncr.entity.UserProfileValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SendVerificationHook extends AbstractSendMailHook {
    public SendVerificationHook() {
    }

    protected Map<String, Object> createContext(RestContext context) {
        Map<String, Object> variables = new HashMap<>();

        context.getValue(User.class).ifPresent(user -> {
            variables.put("baseUrl", Env.getString("EMAIL_BASE_URL", "http://localhost:3000/bouncr/api"));
            variables.put("user", user);
            variables.put("email", user.getUserProfileValues().stream()
                    .filter(v -> Objects.equals(v.getUserProfileField().getJsonName(), "email"))
                    .map(UserProfileValue::getValue)
                    .findAny()
                    .orElse("Unknown"));
        });

        context.getValue(VerificationTargetSet.class).ifPresent(verifications -> {
            verifications.stream()
                    .filter(verification -> verification.getUserProfileField().getJsonName().equals("email"))
                    .findAny()
                    .ifPresent(verification -> variables.put("code", verification.getCode()));
        });
        return variables;
    }

    @Override
    protected Optional<UserProfileValue> findEmailField(RestContext context) {
        return context.getValue(VerificationTargetSet.class).flatMap(verificationTargetSet -> verificationTargetSet.stream()
                .filter(v -> v.getUserProfileField().getJsonName().equals("email"))
                .map(v -> {
                    UserProfileField emailField = v.getUserProfileField();
                    return context.getValue(User.class)
                            .map(User::getUserProfileValues)
                            .map(values -> values.stream()
                                    .filter(value -> value.getUserProfileField().equals(emailField))
                                    .findAny()
                                    .orElse(null))
                            .orElse(null);
                })
                .findAny());
    }

    @Override
    public String getMetaKey() {
        return "Verification";
    }
}
