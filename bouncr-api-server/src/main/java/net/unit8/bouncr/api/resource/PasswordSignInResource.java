package net.unit8.bouncr.api.resource;

import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.PasswordSignIn;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.api.service.UserLockService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserSession;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.util.Arrays;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.EXPIRED;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.INITIAL;
import static net.unit8.bouncr.data.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.bouncr.data.ActionType.USER_SIGNIN;

@AllowedMethods("POST")
public class PasswordSignInResource {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordSignInResource.class);
    static final ContextKey<PasswordSignIn> SIGN_IN_REQ = ContextKey.of(PasswordSignIn.class);
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<UserSession> SESSION = ContextKey.of(UserSession.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        return switch (BouncrJsonDecoders.PASSWORD_SIGN_IN.decode(body)) {
            case Ok<PasswordSignIn> ok -> { context.put(SIGN_IN_REQ, ok.value()); yield null; }
            case Err<PasswordSignIn>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean authenticate(PasswordSignIn signInRequest,
                                ActionRecord actionRecord,
                                RestContext context,
                                DSLContext dsl) {
        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_IN, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return false;
        }
        SignInService signInService = new SignInService(dsl, storeProvider, config);
        UserLockService userLockService = new UserLockService(dsl, config);
        UserRepository userRepo = new UserRepository(dsl);

        User user = userRepo.findByAccountForSignIn(signInRequest.account()).orElse(null);

        if (user != null && user.userLock() != null) {
            context.setMessage(Problem.valueOf(401, "Account is locked", BouncrProblem.ACCOUNT_IS_LOCKED.problemUri()));
            return false;
        }

        if (user != null) {
            actionRecord.setActor(user.account());
            if (user.passwordCredential() != null &&
                    Arrays.equals(
                            user.passwordCredential().password(),
                            PasswordUtils.pbkdf2(
                                    signInRequest.password(),
                                    user.passwordCredential().salt(),
                                    600_000))) {
                context.put(USER, user);
                actionRecord.setActionType(USER_SIGNIN);
                SignInService.PasswordCredentialStatus status = signInService.validatePasswordCredentialAttributes(user);
                if (status == EXPIRED || status == INITIAL) {
                    context.setMessage(Problem.valueOf(401, "Password must be changed", BouncrProblem.PASSWORD_MUST_BE_CHANGED.problemUri()));
                    return false;
                }

                if (!signInService.validateOtpKey(user.otpKey(), signInRequest.oneTimePassword())) {
                    context.setMessage(Problem.valueOf(401, "One time password is needed", BouncrProblem.ONE_TIME_PASSWORD_IS_NEEDED.problemUri()));
                    return false;
                }
                return true;
            } else {
                actionRecord.setActionType(USER_FAILED_SIGNIN);
                userLockService.lockUser(user);
            }
        }
        return false;
    }

    @Decision(ALLOWED)
    public boolean allowed(RestContext context) {
        config.getHookRepo().runHook(HookPoint.ALLOWED_SIGN_IN, context);
        return !context.getMessage().filter(Problem.class::isInstance).isPresent();
    }

    @Decision(POST)
    public boolean doPost(User user, HttpRequest request, RestContext context, DSLContext dsl) {
        SignInService signInService = new SignInService(dsl, storeProvider, config);
        String token = signInService.createToken();
        UserSession userSession = signInService.createUserSession(request, user, token);
        context.put(SESSION, userSession);
        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_IN, context);
        return true;
    }

    @Decision(HANDLE_CREATED)
    public UserSession handleCreated(UserSession userSession) {
        return userSession;
    }
}
