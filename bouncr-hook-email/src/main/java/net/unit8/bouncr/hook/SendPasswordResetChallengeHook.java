package net.unit8.bouncr.hook;

import enkan.Env;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.PasswordResetChallenge;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SendPasswordResetChallengeHook extends AbstractSendMailHook {
    @Override
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

        context.getValue(PasswordResetChallenge.class).ifPresent(challenge-> {
            variables.put("code", challenge.getCode());
        });
        return variables;
    }

    @Override
    protected String getMetaKey() {
        return "PasswordResetChallenge";
    }

    @Override
    protected Optional<UserProfileValue> findEmailField(RestContext context) {
        return context.getValue(User.class).map(user -> user.getUserProfileValues()
                .stream()
                .filter(userProfileValue -> Objects.equals(userProfileValue.getUserProfileField().getJsonName(), "email"))
                .findAny()
                .orElse(null));
    }
}
