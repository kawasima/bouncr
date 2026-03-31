package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.util.PrincipalUtils;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.User;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import net.unit8.bouncr.util.RandomUtils;
import org.jooq.DSLContext;

import jakarta.inject.Inject;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class OtpKeyResource {
    static final ContextKey<User> USER = ContextKey.of(User.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        if (PrincipalUtils.isClientToken(principal)) return false;
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:read") || principal.hasPermission("my:update");
    }

    @Decision(value = ALLOWED, method = {"PUT", "DELETE"})
    public boolean allowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:update");
    }

    @Decision(EXISTS)
    public boolean exists(UserPermissionPrincipal principal, RestContext context, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        var user = userRepo.findByAccount(principal.getName());
        user.ifPresent(u -> context.put(USER, u));
        return user.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(User user, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        return userRepo.findOtpKey(user.id())
                .map(otp -> Map.<String, Object>of("key", Base64.getEncoder().encodeToString(otp.key())))
                .orElseGet(() -> {
                    var m = new HashMap<String, Object>();
                    m.put("key", null);
                    return m;
                });
    }

    @Decision(PUT)
    public Map<String, Object> create(User user, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        byte[] key = RandomUtils.generateRandomString(20, config.getSecureRandom()).getBytes();
        userRepo.insertOtpKey(user.id(), key);
        actionRecord.setActionType(ActionType.OTP_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(principal.getName());
        return Map.of("key", Base64.getEncoder().encodeToString(key));
    }

    @Decision(DELETE)
    public Void delete(User user, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        userRepo.deleteOtpKey(user.id());
        actionRecord.setActionType(ActionType.OTP_DELETED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(principal.getName());
        return null;
    }
}
