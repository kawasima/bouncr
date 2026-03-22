package net.unit8.bouncr.api.resource;

import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.PasswordResetChallengeCreate;
import net.unit8.bouncr.api.repository.PasswordResetChallengeRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.PasswordResetChallenge;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

/**
 * Password Reset.
 *
 * Anonymous user can call this endpoint.
 */
@AllowedMethods({"GET", "POST"})
public class PasswordResetChallengeResource {
    static final ContextKey<PasswordResetChallengeCreate> CREATE_REQ = ContextKey.of(PasswordResetChallengeCreate.class);
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<PasswordResetChallenge> CHALLENGE = ContextKey.of(PasswordResetChallenge.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.PASSWORD_RESET_CHALLENGE_CREATE.decode(body)) {
            case Ok<PasswordResetChallengeCreate> ok -> {
                context.put(CREATE_REQ, ok.value());
                config.getHookRepo().runHook(HookPoint.BEFORE_PASSWORD_RESET_CHALLENGE, context);
                yield null;
            }
            case Err<PasswordResetChallengeCreate>(var issues) -> toProblem(issues);
        };
    }

    /**
     * Create a code for reset password.
     *
     * If the account does not exist, we return the same 201 response, but no password reset
     * challenge is created and the AFTER hook is not run, to prevent account enumeration attacks.
     *
     * @param createRequest a creation request for the password reset challenge
     * @param context a REST context
     * @param dsl a jOOQ DSL context
     * @return null
     */
    @Decision(POST)
    public Void create(PasswordResetChallengeCreate createRequest,
                       RestContext context,
                       DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        Optional<User> userOpt = userRepo.findByAccount(createRequest.account());
        if (userOpt.isEmpty()) {
            // Dummy hash to reduce timing side-channel (CPU-bound only, no persistence).
            RandomUtils.generateRandomString(8);
            return null;
        }
        User user = userOpt.get();
        PasswordResetChallengeRepository repo = new PasswordResetChallengeRepository(dsl);

        String code = RandomUtils.generateRandomString(8);
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMinutes(120));

        PasswordResetChallenge challenge = repo.insert(user.id(), code, expiresAt);
        context.put(USER, user);
        context.put(CHALLENGE, challenge);

        config.getHookRepo().runHook(HookPoint.AFTER_PASSWORD_RESET_CHALLENGE, context);

        return null;
    }
}
