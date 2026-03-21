package net.unit8.bouncr.api.service;

import enkan.data.HttpRequest;
import net.unit8.bouncr.api.authn.OneTimePasswordGenerator;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.UserSessionRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OtpKey;
import net.unit8.bouncr.data.PasswordCredential;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserSession;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.REFRESH_TOKEN;

public class SignInService {
    private final BouncrConfiguration config;
    private final DSLContext dsl;
    private final StoreProvider storeProvider;

    private static final Logger LOG = LoggerFactory.getLogger(SignInService.class);

    public SignInService(DSLContext dsl, StoreProvider storeProvider, BouncrConfiguration config) {
        this.config = config;
        this.storeProvider = storeProvider;
        this.dsl = dsl;
    }

    public boolean validateOtpKey(OtpKey otpKey, String code) {
        if (otpKey == null) return true;

        return new OneTimePasswordGenerator(30)
                .generateTotpSet(otpKey.key(), 5)
                .stream()
                .map(n -> String.format(Locale.US, "%06d", n))
                .collect(Collectors.toSet())
                .contains(code);
    }

    public PasswordCredentialStatus validatePasswordCredentialAttributes(User user) {
        PasswordCredential passwordCredential = user.passwordCredential();
        if (passwordCredential.initial()) {
            return INITIAL;
        }

        if (config.getPasswordPolicy().getExpires() != null) {
            Instant createdAt = passwordCredential.createdAt().toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
            return (createdAt.plus(config.getPasswordPolicy().getExpires()).isBefore(config.getClock().instant())) ?
                    EXPIRED : VALID;
        }

        return VALID;
    }

    public String createToken() {
        return UUID.randomUUID().toString();
    }

    public UserSession createUserSession(HttpRequest request, User user, String token) {
        String userAgent = some(request.getHeaders().get("User-Agent"),
                ua -> ua.substring(0, Math.min(ua.length(), 255))).orElse("");

        UserSessionRepository sessionRepo = new UserSessionRepository(dsl);
        UserSession userSession = sessionRepo.insert(user.id(), token, request.getRemoteAddr(), userAgent, LocalDateTime.now());

        UserRepository userRepo = new UserRepository(dsl);
        HashMap<String, Object> profileMap = new HashMap<>(
                userRepo.loadProfileValues(user.id()).stream()
                        .collect(Collectors.toMap(v -> v.userProfileField().jsonName(), v -> v.value())));
        profileMap.put("iss", "bouncr");
        profileMap.put("uid", Long.toString(user.id()));
        profileMap.put("sub", user.account());
        profileMap.put("permissionsByRealm", userRepo.getPermissionsByRealm(user.id()));
        LOG.debug("signIn profileMap = {}", profileMap);
        storeProvider.getStore(BOUNCR_TOKEN).write(token, profileMap);

        // Write refresh token marker (long-lived session)
        HashMap<String, Object> refreshData = new HashMap<>();
        refreshData.put("userId", user.id());
        storeProvider.getStore(REFRESH_TOKEN).write(token, refreshData);

        return userSession;
    }

    /**
     * Rebuild the profileMap from DB for the given userId.
     * Called by TokenRefreshResource when the access token cache has expired.
     */
    public HashMap<String, Object> refreshAccessToken(long userId, String sessionId) {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return null;

        HashMap<String, Object> profileMap = new HashMap<>(
                userRepo.loadProfileValues(userId).stream()
                        .collect(Collectors.toMap(v -> v.userProfileField().jsonName(), v -> v.value())));
        profileMap.put("iss", "bouncr");
        profileMap.put("uid", Long.toString(userId));
        profileMap.put("sub", user.account());
        profileMap.put("permissionsByRealm", userRepo.getPermissionsByRealm(userId));

        storeProvider.getStore(BOUNCR_TOKEN).write(sessionId, profileMap);

        // Re-write refresh token marker to extend TTL (sliding window)
        HashMap<String, Object> refreshData = new HashMap<>();
        refreshData.put("userId", userId);
        storeProvider.getStore(REFRESH_TOKEN).write(sessionId, refreshData);

        LOG.debug("refreshed profileMap for user {} session {}", user.account(), sessionId);
        return profileMap;
    }

    public enum PasswordCredentialStatus {
        VALID,
        INITIAL,
        EXPIRED
    }
}
