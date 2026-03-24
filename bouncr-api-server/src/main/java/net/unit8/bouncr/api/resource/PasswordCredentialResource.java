package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.PasswordCredential;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.WordName;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple2;
import net.unit8.raoh.combinator.Tuple3;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.bouncr.component.config.HookPoint.*;

@AllowedMethods({"POST", "PUT", "DELETE"})
public class PasswordCredentialResource {
    // POST: Tuple3<WordName, String, Boolean> (account, password, initial)
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple3<WordName, String, Boolean>> CREATE_REQ =
            (ContextKey<Tuple3<WordName, String, Boolean>>) (ContextKey<?>) ContextKey.of("createReq", Tuple3.class);
    // PUT: Tuple3<String, String, String> (account, oldPassword, newPassword)
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple3<String, String, String>> UPDATE_REQ =
            (ContextKey<Tuple3<String, String, String>>) (ContextKey<?>) ContextKey.of("updateReq", Tuple3.class);
    // DELETE: Tuple2<WordName, String> (account, password)
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple2<WordName, String>> DELETE_REQ =
            (ContextKey<Tuple2<WordName, String>>) (ContextKey<?>) ContextKey.of(Tuple2.class);
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<PasswordCredential> CREDENTIAL = ContextKey.of(PasswordCredential.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreateRequest(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        return switch (BouncrJsonDecoders.PASSWORD_CREDENTIAL_CREATE.decode(body)) {
            case Ok(Tuple3(var account, var password, var initial)) -> {
                // Validate password policy
                Problem.Violation policyViolation = conformPolicy((String) password);
                if (policyViolation != null) {
                    yield Problem.fromViolationList(java.util.List.of(policyViolation));
                }
                context.put(CREATE_REQ, new Tuple3<>((WordName) account, (String) password, (Boolean) initial));
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException();
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        return switch (BouncrJsonDecoders.PASSWORD_CREDENTIAL_UPDATE.decode(body)) {
            case Ok(Tuple3(var account, var oldPassword, var newPassword)) -> {
                if (Objects.equals(newPassword, oldPassword)) {
                    yield Problem.fromViolationList(java.util.List.of(
                            new Problem.Violation("new_password", "is the same as the old password")));
                }
                Problem.Violation policyViolation = conformPolicy((String) newPassword);
                if (policyViolation != null) {
                    yield Problem.fromViolationList(java.util.List.of(policyViolation));
                }
                context.put(UPDATE_REQ, new Tuple3<>((String) account, (String) oldPassword, (String) newPassword));
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException();
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(value = MALFORMED, method = "DELETE")
    public Problem validateDeleteRequest(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        return switch (BouncrJsonDecoders.PASSWORD_CREDENTIAL_DELETE.decode(body)) {
            case Ok(Tuple2(var account, var password)) -> {
                context.put(DELETE_REQ, new Tuple2<>((WordName) account, (String) password));
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException();
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(value = AUTHORIZED, method = {"POST", "DELETE"})
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal, Tuple3<WordName, String, Boolean> createRequest) {
        if (principal.hasPermission("any_user:update") || principal.hasPermission("user:update")) {
            return true;
        }
        return principal.getName().equals(createRequest._1().value());
    }

    /**
     * PUT (password change) allows unauthenticated access because the old password
     * serves as proof of identity. This is required for initial password changes
     * where the user cannot sign in (PASSWORD_MUST_BE_CHANGED).
     * The actual old-password verification happens in the PUT handler.
     */
    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal, Tuple3<String, String, String> updateRequest) {
        if (principal == null) {
            // Unauthenticated: allowed only for self-service password change (old password verified in handler)
            return updateRequest._1() != null;
        }
        if (principal.hasPermission("any_user:update") || principal.hasPermission("user:update")) {
            return true;
        }
        return updateRequest._1() == null || principal.getName().equals(updateRequest._1());
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal, Tuple2<WordName, String> deleteRequest) {
        if (principal.hasPermission("any_user:delete") || principal.hasPermission("user:delete")) {
            return true;
        }
        return principal.getName().equals(deleteRequest._1().value());
    }

    @Decision(value = PROCESSABLE, method = "POST")
    public boolean userProcessableInPost(Tuple3<WordName, String, Boolean> createRequest, RestContext context, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        return userRepo.findByAccount(createRequest._1().value())
                .map(user -> {
                    context.put(USER, user);
                    return user;
                }).isPresent();
    }

    @Decision(value = PROCESSABLE, method = "PUT")
    public boolean userProcessableInPut(Tuple3<String, String, String> updateRequest,
                                        UserPermissionPrincipal principal,
                                        RestContext context,
                                        DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        String account = updateRequest._1() != null ? updateRequest._1() :
                (principal != null ? principal.getName() : null);
        if (account == null) return false;

        return userRepo.findByAccountForSignIn(account)
                .filter(creds -> creds.passwordCredential() != null)
                .filter(creds ->
                        Arrays.equals(creds.passwordCredential().password(),
                                PasswordUtils.pbkdf2(updateRequest._2(), creds.passwordCredential().salt(), config.getPbkdf2Iterations()))
                )
                .flatMap(creds -> userRepo.findById(creds.id()))
                .map(user -> {
                    context.put(USER, user);
                    return user;
                })
                .isPresent();
    }

    @Decision(POST)
    public PasswordCredential create(Tuple3<WordName, String, Boolean> createRequest,
                                     User user,
                                     UserPermissionPrincipal principal,
                                     ActionRecord actionRecord,
                                     RestContext context,
                                     DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        byte[] hash = PasswordUtils.pbkdf2(createRequest._2(), salt, config.getPbkdf2Iterations());

        userRepo.insertPasswordCredential(user.id(), hash, salt, createRequest._3());

        PasswordCredential passwordCredential = new PasswordCredential(null, hash, salt, createRequest._3(), LocalDateTime.now());
        context.put(CREDENTIAL, passwordCredential);
        config.getHookRepo().runHook(AFTER_CREATE_PASSWORD_CREDENTIAL, context);

        actionRecord.setActionType(ActionType.PASSWORD_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(user.account());

        return passwordCredential;
    }

    @Decision(PUT)
    public PasswordCredential update(Tuple3<String, String, String> updateRequest,
                                     User user,
                                     UserPermissionPrincipal principal,
                                     ActionRecord actionRecord,
                                     RestContext context,
                                     DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        byte[] hash = PasswordUtils.pbkdf2(updateRequest._3(), salt, config.getPbkdf2Iterations());

        userRepo.deletePasswordCredential(user.id());
        userRepo.insertPasswordCredential(user.id(), hash, salt, false);

        PasswordCredential passwordCredential = new PasswordCredential(null, hash, salt, false, LocalDateTime.now());
        context.put(CREDENTIAL, passwordCredential);
        config.getHookRepo().runHook(AFTER_UPDATE_PASSWORD_CREDENTIAL, context);

        actionRecord.setActionType(ActionType.PASSWORD_CHANGED);
        actionRecord.setActor(Optional.ofNullable(principal)
                .map(Principal::getName)
                .orElse(user.account()));
        actionRecord.setDescription(user.account());

        return passwordCredential;
    }

    @Decision(DELETE)
    public Void delete(UserPermissionPrincipal principal,
                       ActionRecord actionRecord,
                       RestContext context,
                       DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        userRepo.findByAccount(principal.getName()).ifPresent(user -> {
            userRepo.deletePasswordCredential(user.id());
            config.getHookRepo().runHook(AFTER_DELETE_PASSWORD_CREDENTIAL, context);
        });

        actionRecord.setActionType(ActionType.PASSWORD_DELETED);
        actionRecord.setActor(principal.getName());

        return null;
    }

    private Problem.Violation conformPolicy(String password) {
        var policy = config.getPasswordPolicy();
        int passwordLen = password != null ? password.length() : 0;
        if (passwordLen > policy.getMaxLength()) {
            return new Problem.Violation("password", "must be less than " + policy.getMaxLength() + " characters");
        }
        if (passwordLen < policy.getMinLength()) {
            return new Problem.Violation("password", "must be greater than " + policy.getMinLength() + " characters");
        }
        if (policy.getPattern() != null && !policy.getPattern().matcher(password).matches()) {
            return new Problem.Violation("password", "doesn't match pattern");
        }
        return null;
    }
}
