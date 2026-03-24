package net.unit8.bouncr.api.resource;

import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.PasswordResetChallengeRepository;
import net.unit8.bouncr.api.service.PasswordCredentialService;
import net.unit8.bouncr.api.service.UserLockService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.InitialPassword;
import net.unit8.bouncr.data.PasswordResetChallenge;
import net.unit8.bouncr.data.User;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"PUT"})
public class PasswordResetResource {
    static final ContextKey<InitialPassword> INITIAL_PASSWORD = ContextKey.of(InitialPassword.class);
    static final ContextKey<String> PASSWORD_RESET_REQUEST = ContextKey.of("passwordResetCode", String.class);
    static final ContextKey<PasswordResetChallenge> RESET_CHALLENGE = ContextKey.of(PasswordResetChallenge.class);
    static final ContextKey<User> USER = ContextKey.of(User.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.PASSWORD_RESET.decode(body)) {
            case Ok(String code) -> { context.put(PASSWORD_RESET_REQUEST, code); yield null; }
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(PROCESSABLE)
    public boolean existsCode(String resetRequest,
                              RestContext context,
                              DSLContext dsl) {
        PasswordResetChallengeRepository repo = new PasswordResetChallengeRepository(dsl);
        var challenge = repo.findActiveByCode(resetRequest);
        challenge.ifPresent(c -> {
            context.put(RESET_CHALLENGE, c);
            context.put(USER, c.user());
        });
        return challenge.isPresent();
    }

    @Decision(PUT)
    public InitialPassword reset(PasswordResetChallenge resetChallenge,
                                 User user,
                                 ActionRecord actionRecord,
                                 RestContext context,
                                 DSLContext dsl) {
        PasswordResetChallengeRepository challengeRepo = new PasswordResetChallengeRepository(dsl);
        PasswordCredentialService passwordCredentialService = new PasswordCredentialService(dsl, config);
        UserLockService userLockService = new UserLockService(dsl, config);

        challengeRepo.delete(resetChallenge.id());
        userLockService.unlockUser(user);
        InitialPassword initialPassword = passwordCredentialService.initializePassword(user);
        context.put(INITIAL_PASSWORD, initialPassword);

        actionRecord.setActionType(ActionType.PASSWORD_CHANGED);
        actionRecord.setActor(user.account());
        config.getHookRepo().runHook(HookPoint.AFTER_PASSWORD_RESET, context);

        return initialPassword;
    }
}
