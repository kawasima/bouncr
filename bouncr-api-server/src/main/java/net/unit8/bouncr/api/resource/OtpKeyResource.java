package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
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
    @Inject
    private BouncrConfiguration config;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = {"PUT", "DELETE"})
    public boolean allowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:update");
    }

    @Decision(HANDLE_OK)
    public OtpKey find(UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        return userRepo.findByAccount(principal.getName())
                .flatMap(user -> userRepo.findOtpKey(user.id()))
                .orElse(new OtpKey(null));
    }

    @Decision(PUT)
    public OtpKey create(UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.findByAccount(principal.getName()).orElseThrow();
        byte[] key = RandomUtils.generateRandomString(20, config.getSecureRandom()).getBytes();
        userRepo.insertOtpKey(user.id(), key);
        return new OtpKey(key);
    }

    @Decision(DELETE)
    public Void delete(UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        userRepo.findByAccount(principal.getName())
                .ifPresent(user -> userRepo.deleteOtpKey(user.id()));
        return null;
    }
}
