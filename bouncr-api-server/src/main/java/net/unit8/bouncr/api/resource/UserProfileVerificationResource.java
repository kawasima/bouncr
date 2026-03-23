package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.UserProfileVerificationRepository;
import net.unit8.bouncr.data.UserProfileVerification;
import org.jooq.DSLContext;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods("DELETE")
public class UserProfileVerificationResource {
    static final ContextKey<UserProfileVerification> VERIFICATION = ContextKey.of(UserProfileVerification.class);

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        UserProfileVerificationRepository repo = new UserProfileVerificationRepository(dsl);
        var maybeVerification = repo.findByCode(params.get("code"));
        maybeVerification.ifPresent(v -> context.put(VERIFICATION, v));
        return maybeVerification.isPresent();
    }

    @Decision(DELETE)
    public Void delete(UserProfileVerification userProfileVerification, DSLContext dsl) {
        UserProfileVerificationRepository repo = new UserProfileVerificationRepository(dsl);
        repo.delete(userProfileVerification.id().userProfileField(), userProfileVerification.id().user());
        return null;
    }
}
