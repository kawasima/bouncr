package net.unit8.bouncr.api.resource;

import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.verifier.exception.VerificationException;
import enkan.collection.Headers;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.util.BouncrCookies;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.WebAuthnSignInResponse;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.WebAuthnAuthenticate;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.WebAuthnCredentialRepository;
import net.unit8.bouncr.component.AuthFailureTracker;
import net.unit8.bouncr.api.service.SignInService;
import net.unit8.bouncr.api.service.WebAuthnService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserSession;
import net.unit8.bouncr.data.WebAuthnChallenge;
import net.unit8.bouncr.data.WebAuthnCredential;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.bouncr.component.StoreProvider.StoreType.WEBAUTHN_CHALLENGE;
import static net.unit8.bouncr.data.ActionType.USER_SIGNIN;

@AllowedMethods("POST")
public class WebAuthnSignInResource {
    private static final Logger LOG = LoggerFactory.getLogger(WebAuthnSignInResource.class);
    private static final String COOKIE_NAME = "WEBAUTHN_SESSION_ID";

    static final ContextKey<WebAuthnAuthenticate> AUTH_REQ = ContextKey.of(WebAuthnAuthenticate.class);
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
        return switch (BouncrJsonDecoders.WEBAUTHN_AUTHENTICATE.decode(body)) {
            case Ok<WebAuthnAuthenticate> ok -> { context.put(AUTH_REQ, ok.value()); yield null; }
            case Err<WebAuthnAuthenticate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean authenticate(WebAuthnAuthenticate authRequest,
                                HttpRequest request,
                                ActionRecord actionRecord,
                                RestContext context,
                                DSLContext dsl) {
        String ip = request.getRemoteAddr();
        if (authFailureTracker.isBlocked(ip, null)) {
            context.setMessage(Problem.valueOf(429, "Too many failed attempts",
                    BouncrProblem.TOO_MANY_REQUESTS.problemUri()));
            return false;
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_IN, context);
        if (context.getMessage().filter(Problem.class::isInstance).isPresent()) {
            return false;
        }

        String sessionId = some(request.getCookies().get(COOKIE_NAME), Cookie::getValue).orElse(null);
        if (sessionId == null) {
            context.setMessage(Problem.valueOf(401, "WebAuthn session not found",
                    BouncrProblem.WEBAUTHN_CHALLENGE_EXPIRED.problemUri()));
            return false;
        }

        WebAuthnChallenge challengeData = (WebAuthnChallenge) storeProvider.getStore(WEBAUTHN_CHALLENGE).read(sessionId);
        if (challengeData == null) {
            context.setMessage(Problem.valueOf(401, "Challenge expired",
                    BouncrProblem.WEBAUTHN_CHALLENGE_EXPIRED.problemUri()));
            return false;
        }
        storeProvider.getStore(WEBAUTHN_CHALLENGE).delete(sessionId);

        if (!WebAuthnChallenge.TYPE_AUTHENTICATION.equals(challengeData.type())) {
            context.setMessage(Problem.valueOf(401, "Invalid challenge type",
                    BouncrProblem.WEBAUTHN_VERIFICATION_FAILED.problemUri()));
            return false;
        }

        WebAuthnService webAuthnService = new WebAuthnService(config);
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        UserRepository userRepo = new UserRepository(dsl);

        AuthenticationData authData;
        try {
            authData = webAuthnService.getWebAuthnManager()
                    .parseAuthenticationResponseJSON(authRequest.authenticationResponseJSON());
        } catch (DataConversionException e) {
            LOG.warn("Failed to parse WebAuthn authentication response", e);
            authFailureTracker.recordFailure(ip, null);
            context.setMessage(Problem.valueOf(401, "Invalid authentication response",
                    BouncrProblem.WEBAUTHN_VERIFICATION_FAILED.problemUri()));
            return false;
        }

        byte[] credentialIdBytes = authData.getCredentialId();
        WebAuthnCredential storedCredential = credRepo.findByCredentialId(credentialIdBytes).orElse(null);
        if (storedCredential == null) {
            authFailureTracker.recordFailure(ip, null);
            context.setMessage(Problem.valueOf(401, "Credential not found",
                    BouncrProblem.WEBAUTHN_CREDENTIAL_NOT_FOUND.problemUri()));
            return false;
        }
        Long userId = storedCredential.userId();

        if (challengeData.userId() != null && !challengeData.userId().equals(userId)) {
            context.setMessage(Problem.valueOf(401, "Credential does not match requested user",
                    BouncrProblem.WEBAUTHN_VERIFICATION_FAILED.problemUri()));
            return false;
        }

        try {
            webAuthnService.verifyAuthenticationData(
                    authData,
                    challengeData.challenge(),
                    storedCredential);
        } catch (VerificationException e) {
            LOG.warn("WebAuthn assertion verification failed", e);
            authFailureTracker.recordFailure(ip, null);
            context.setMessage(Problem.valueOf(401, "Verification failed",
                    BouncrProblem.WEBAUTHN_VERIFICATION_FAILED.problemUri()));
            return false;
        }

        credRepo.updateSignCount(storedCredential.id(),
                authData.getAuthenticatorData().getSignCount());

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            context.setMessage(Problem.valueOf(401, "User not found",
                    BouncrProblem.WEBAUTHN_VERIFICATION_FAILED.problemUri()));
            return false;
        }

        if (user.userLock() != null) {
            context.setMessage(Problem.valueOf(401, "Account is locked",
                    BouncrProblem.ACCOUNT_IS_LOCKED.problemUri()));
            return false;
        }

        context.put(USER, user);
        actionRecord.setActor(user.account());
        actionRecord.setActionType(USER_SIGNIN);
        return true;
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
    public ApiResponse handleCreated(User user, UserSession userSession) {
        BouncrCookies cookies = new BouncrCookies(config);
        String tokenCookie = cookies.token(userSession.token()).toHttpString();
        String clearSessionCookie = cookies.clearSession(COOKIE_NAME).toHttpString();

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 201)
                .set(ApiResponse::setHeaders, Headers.of(
                        "Set-Cookie", tokenCookie,
                        "Set-Cookie", clearSessionCookie))
                .set(ApiResponse::setBody, new WebAuthnSignInResponse(
                        userSession.token(), user.account()))
                .build();
    }
}
