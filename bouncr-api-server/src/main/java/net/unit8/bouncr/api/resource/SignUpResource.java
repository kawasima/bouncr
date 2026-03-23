package net.unit8.bouncr.api.resource;

import tools.jackson.core.type.TypeReference;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.SignUpResponse;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.SignUp;
import net.unit8.bouncr.api.repository.InvitationRepository;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.service.PasswordCredentialService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.*;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple2;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.raoh.json.JsonDecoders.combine;

@AllowedMethods({"POST"})
public class SignUpResource {
    static final ContextKey<SignUp> SIGN_UP_REQ = ContextKey.of(SignUp.class);
    static final ContextKey<UserProfile> USER_PROFILE = ContextKey.of(UserProfile.class);
    static final ContextKey<User> USER_KEY = ContextKey.of(User.class);
    static final ContextKey<VerificationTargetSet> VERIFICATION_TARGET_SET = ContextKey.of(VerificationTargetSet.class);
    static final ContextKey<InitialPassword> INITIAL_PASSWORD = ContextKey.of(InitialPassword.class);

    @Inject
    private JsonWebToken jsonWebToken;

    @Inject
    private BouncrConfiguration config;

    @Decision(MALFORMED)
    public Problem validate(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }

        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        var result = combine(BouncrJsonDecoders.SIGN_UP, BouncrJsonDecoders.userProfile(fieldRepo))
                .map(Tuple2::new)
                .decode(body);

        return switch (result) {
            case Ok(Tuple2(SignUp signUp, UserProfile profile)) -> {
                config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, profile.values());
                context.put(SIGN_UP_REQ, signUp);
                context.put(USER_PROFILE, profile);
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException("Unexpected Ok value");
            case Err(var issues) -> toProblem(issues);
        };
    }

    @Decision(ALLOWED)
    public boolean isAllowedSignUp() {
        return config.isSignUpEnabled();
    }

    @Decision(HANDLE_FORBIDDEN)
    public Problem forbidden() {
        return Problem.valueOf(403, "Signing up is NOT allowed");
    }

    @Decision(CONFLICT)
    public boolean conflict(SignUp signUp, UserProfile userProfile, RestContext context, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        List<Problem.Violation> violations = new ArrayList<>();

        if (!userRepo.isAccountUnique(signUp.account())) {
            violations.add(new Problem.Violation("account", "conflicts"));
        }

        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        List<UserProfileField> identityFields = fieldRepo.findIdentityFields();

        for (UserProfileField f : identityFields) {
            userProfile.get(f.jsonName()).ifPresent(value -> {
                Optional<Long> existingUserId = userRepo.findUserIdByProfileIdentity(f.jsonName(), value);
                if (existingUserId.isPresent()) {
                    violations.add(new Problem.Violation(f.jsonName(), "conflicts"));
                }
            });
        }

        if (!violations.isEmpty()) {
            context.setMessage(Problem.valueOf(409));
        }
        return !violations.isEmpty();
    }

    @Decision(POST)
    public SignUpResponse create(SignUp signUp,
                                UserProfile userProfile,
                                RestContext context,
                                DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        InvitationRepository invitationRepo = new InvitationRepository(dsl);

        User user = userRepo.insert(signUp.account());
        context.put(USER_KEY, user);

        List<UserProfileField> verificationFields = new ArrayList<>();

        for (Map.Entry<String, String> entry : userProfile.values().entrySet()) {
            fieldRepo.findByJsonName(entry.getKey()).ifPresent(field -> {
                userRepo.setProfileValue(user.id(), field.id(), entry.getValue());
                if (field.needsVerification()) {
                    verificationFields.add(field);
                }
            });
        }

        List<UserProfileVerification> profileVerifications = verificationFields.stream()
                .map(f -> {
                    String verificationCode = RandomUtils.generateRandomString(6);
                    LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
                    userRepo.insertProfileVerification(f.id(), user.id(), verificationCode, expiresAt);
                    return new UserProfileVerification(
                            new UserProfileVerificationId(f.id(), user.id()),
                            verificationCode, expiresAt);
                })
                .collect(Collectors.toList());

        if (!profileVerifications.isEmpty()) {
            context.put(VERIFICATION_TARGET_SET, new VerificationTargetSet(profileVerifications));
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_UP, context);

        Optional<Invitation> invitation = Optional.ofNullable(signUp.code())
                .flatMap(invitationRepo::findByCode);

        invitation.ifPresent(invi -> {
            if (invi.groupInvitations() != null) {
                for (GroupInvitation gi : invi.groupInvitations()) {
                    userRepo.addToGroup(user.id(), gi.group().id());
                }
            }

            if (invi.oidcInvitations() != null) {
                for (OidcInvitation oidcInvitation : invi.oidcInvitations()) {
                    JwtClaim claim = jsonWebToken.decodePayload(oidcInvitation.oidcPayload(), new TypeReference<JwtClaim>() {});
                    userRepo.insertOidcUser(oidcInvitation.id(), user.id(), claim.getSub());
                }
            }

            invitationRepo.delete(invi.code());
        });

        InitialPassword initialPassword = null;
        if (signUp.enablePasswordCredential()) {
            PasswordCredentialService passwordCredentialService = new PasswordCredentialService(dsl, config);
            initialPassword = passwordCredentialService.initializePassword(user);
            context.put(INITIAL_PASSWORD, initialPassword);
        }

        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_UP, context);

        return builder(new SignUpResponse())
                .set(SignUpResponse::setId, user.id())
                .set(SignUpResponse::setAccount, user.account())
                .set(SignUpResponse::setUserProfiles, new HashMap<>(userProfile.values()))
                .set(SignUpResponse::setPassword, initialPassword != null ? initialPassword.password() : null)
                .build();
    }
}
