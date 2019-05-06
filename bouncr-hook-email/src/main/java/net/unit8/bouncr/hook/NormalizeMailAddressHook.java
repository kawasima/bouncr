package net.unit8.bouncr.hook;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NormalizeMailAddressHook implements Hook<Map<String, Object>> {
    @Override
    public void run(Map<String, Object> userProfiles) {
        Optional.ofNullable(userProfiles.get("email"))
                .filter(String.class::isInstance)
                .map(Objects::toString)
                .ifPresent(email -> userProfiles.put("email", normalizeMailAddress(email)));
    }

    private String normalizeMailAddress(String mailAddress) {
        if (mailAddress == null) {
            return null;
        }

        String[] tokens = mailAddress.split("@", 2);
        if (tokens.length < 2) {
            return mailAddress;
        }

        return tokens[0] + "@" + tokens[1].toLowerCase(Locale.US);
    }
}
