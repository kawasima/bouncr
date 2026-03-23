package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OtpKey;
import net.unit8.bouncr.data.User;
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
    public OtpKey find(User user, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        return userRepo.findOtpKey(user.id())
                .orElse(new OtpKey(null));
    }

    @Decision(PUT)
    public OtpKey create(User user, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        byte[] key = RandomUtils.generateRandomString(20, config.getSecureRandom()).getBytes();
        userRepo.insertOtpKey(user.id(), key);
        return new OtpKey(key);
    }

    @Decision(DELETE)
    public Void delete(User user, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        userRepo.deleteOtpKey(user.id());
        return null;
    }
}
