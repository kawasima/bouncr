package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.AuthFailureTracker;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.api.service.UserLockService;
import net.unit8.bouncr.api.util.BouncrCookies;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserCredentials;
import net.unit8.bouncr.data.UserSession;
import net.unit8.bouncr.data.WordName;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple3;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.util.Arrays;

import static enkan.util.BeanBuilder.builder;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.EXPIRED;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.INITIAL;
import static net.unit8.bouncr.data.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.bouncr.data.ActionType.USER_SIGNIN;

@AllowedMethods("POST")
public class PasswordSignInResource {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordSignInResource.class);
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple3<WordName, String, String>> SIGN_IN_REQ =
            (ContextKey<Tuple3<WordName, String, String>>) (ContextKey<?>) ContextKey.of(Tuple3.class);
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<UserSession> SESSION = ContextKey.of(UserSession.class);

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Inject
    private AuthFailureTracker authFailureTracker;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        return switch (BouncrJsonDecoders.PASSWORD_SIGN_IN.decode(body)) {
            case Ok(Tuple3(var account, var password, var otp)) -> {
                context.put(SIGN_IN_REQ, new Tuple3<>((WordName) account, (String) password, (String) otp));
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException();
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean authenticate(Tuple3<WordName, String, String> signInRequest,
                                HttpRequest request,
                                ActionRecord actionRecord,
                                RestContext context,
                                DSLContext dsl) {
        String ip = request.getRemoteAddr();
        String account = signInRequest._1().value();
        if (authFailureTracker.isBlocked(ip, account)) {
            context.setMessage(Problem.valueOf(429, "Too many failed attempts",
                    BouncrProblem.TOO_MANY_REQUESTS.problemUri()));
            return false;
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_IN, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return false;
        }
        SignInService signInService = new SignInService(dsl, storeProvider, config);
        UserLockService userLockService = new UserLockService(dsl, config);
        UserRepository userRepo = new UserRepository(dsl);

        UserCredentials creds = userRepo.findByAccountForSignIn(account).orElse(null);

        // Always perform the hash before any branching to equalize response timing,
        // preventing "account not found" and "account locked" timing-based enumeration.
        // Use the account's own salt when available; fall back to a per-process random
        // dummy salt for unknown accounts or accounts without a password credential.
        String salt = (creds != null && creds.passwordCredential() != null)
                ? creds.passwordCredential().salt()
                : config.getDummySalt();
        byte[] computedHash = PasswordUtils.pbkdf2(signInRequest._2(), salt, config.getPbkdf2Iterations());

        if (creds == null) {
            authFailureTracker.recordFailure(ip, account);
            return false;
        }

        if (creds.userLock() != null) {
            context.setMessage(Problem.valueOf(401, "Account is locked", BouncrProblem.ACCOUNT_IS_LOCKED.problemUri()));
            return false;
        }

        actionRecord.setActor(creds.account());
        if (creds.passwordCredential() != null &&
                Arrays.equals(
                        creds.passwordCredential().password(),
                        computedHash)) {
            User user = userRepo.findById(creds.id()).orElse(null);
            if (user == null) return false;
            context.put(USER, user);
            actionRecord.setActionType(USER_SIGNIN);
            SignInService.PasswordCredentialStatus status = signInService.validatePasswordCredentialAttributes(creds);
            if (status == EXPIRED || status == INITIAL) {
                context.setMessage(Problem.valueOf(401, "Password must be changed", BouncrProblem.PASSWORD_MUST_BE_CHANGED.problemUri()));
                return false;
            }

            if (!signInService.validateOtpKey(creds.otpKey(), signInRequest._3())) {
                context.setMessage(Problem.valueOf(401, "One time password is needed", BouncrProblem.ONE_TIME_PASSWORD_IS_NEEDED.problemUri()));
                return false;
            }
            return true;
        } else {
            actionRecord.setActionType(USER_FAILED_SIGNIN);
            userLockService.lockUser(creds);
            authFailureTracker.recordFailure(ip, account);
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
    public ApiResponse handleCreated(UserSession userSession) {
        String cookie = new BouncrCookies(config).token(userSession.token()).toHttpString();
        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 201)
                .set(ApiResponse::setHeaders, Headers.of("Set-Cookie", cookie))
                .set(ApiResponse::setBody, userSession)
                .build();
    }
}
