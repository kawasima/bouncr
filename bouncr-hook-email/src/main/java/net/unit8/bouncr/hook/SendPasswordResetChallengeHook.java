package net.unit8.bouncr.hook;

import enkan.Env;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.data.PasswordResetChallenge;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfileValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SendPasswordResetChallengeHook extends AbstractSendMailHook {
    @Override
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

        context.getByType(PasswordResetChallenge.class).ifPresent(challenge-> {
            variables.put("code", challenge.code());
        });
        return variables;
    }

    @Override
    protected String getMetaKey() {
        return "PasswordResetChallenge";
    }

    @Override
    protected Optional<UserProfileValue> findEmailField(RestContext context) {
        return context.getByType(User.class).map(user -> user.userProfileValues()
                .stream()
                .filter(userProfileValue -> Objects.equals(userProfileValue.userProfileField().jsonName(), "email"))
                .findAny()
                .orElse(null));
    }
}
