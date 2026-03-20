package net.unit8.bouncr.api.resource;

import tools.jackson.core.type.TypeReference;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.SignUpResponse;
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
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"POST"})
public class SignUpResource {
    static final ContextKey<String> ACCOUNT = ContextKey.of("account", String.class);
    static final ContextKey<String> CODE = ContextKey.of("code", String.class);
    static final ContextKey<Boolean> ENABLE_PASSWORD = ContextKey.of("enablePassword", Boolean.class);
    static final ContextKey<Map> USER_PROFILES = ContextKey.of("userProfiles", Map.class);
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
        // Extract account
        JsonNode accountNode = body.get("account");
        if (accountNode == null || accountNode.asText().isBlank()) {
            return Problem.valueOf(400, "account is required", BouncrProblem.MALFORMED.problemUri());
        }
        String account = accountNode.asText();
        if (account.length() > 100 || !account.matches("^\\w+$")) {
            return Problem.valueOf(400, "account format is invalid", BouncrProblem.MALFORMED.problemUri());
        }
        context.put(ACCOUNT, account);

        // Extract code
        JsonNode codeNode = body.get("code");
        if (codeNode != null && !codeNode.isNull()) {
            context.put(CODE, codeNode.asText());
        }

        // Extract enable_password_credential
        JsonNode enablePwdNode = body.get("enable_password_credential");
        boolean enablePassword = enablePwdNode == null || enablePwdNode.asBoolean(true);
        context.put(ENABLE_PASSWORD, enablePassword);

        // Extract user profile fields dynamically
        Map<String, Object> userProfiles = new HashMap<>();
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        List<UserProfileField> allFields = fieldRepo.findAll();
        Set<String> knownNonProfileFields = new HashSet<>(List.of("account", "enable_password_credential", "code"));

        for (UserProfileField f : allFields) {
            JsonNode fieldNode = body.get(f.jsonName());
            if (fieldNode != null && !fieldNode.isNull()) {
                userProfiles.put(f.jsonName(), fieldNode.asText());
            }
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, userProfiles);

        // Validate profiles
        List<Problem.Violation> profileViolations = new ArrayList<>();
        for (UserProfileField f : allFields) {
            Object value = userProfiles.get(f.jsonName());
            if (value == null) {
                if (f.isRequired()) {
                    profileViolations.add(new Problem.Violation(f.jsonName(), "is required"));
                }
                continue;
            }
            String strValue = Objects.toString(value);
            if (f.maxLength() != null && strValue.length() > f.maxLength()) {
                profileViolations.add(new Problem.Violation(f.jsonName(), "" + f.maxLength()));
            }
            if (f.minLength() != null && strValue.length() < f.minLength()) {
                profileViolations.add(new Problem.Violation(f.jsonName(), "" + f.minLength()));
            }
            if (f.regularExpression() != null && !Pattern.compile(f.regularExpression()).matcher(strValue).matches()) {
                profileViolations.add(new Problem.Violation(f.jsonName(), "" + f.regularExpression()));
            }
        }

        if (!profileViolations.isEmpty()) {
            return Problem.valueOf(400);
        }

        context.put(USER_PROFILES, userProfiles);
        return null;
    }

    @Decision(ALLOWED)
    public boolean isAllowedSignUp() {
        return config.isSignUpEnabled();
    }

    @Decision(HANDLE_FORBIDDEN)
    public Problem forbidden() {
        return Problem.valueOf(403, "Signing up is NOT allowed");
    }

    @SuppressWarnings("unchecked")
    @Decision(CONFLICT)
    public boolean conflict(String account, Map userProfiles, RestContext context, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        List<Problem.Violation> violations = new ArrayList<>();

        // Check account uniqueness
        if (!userRepo.isAccountUnique(account)) {
            violations.add(new Problem.Violation("account", "conflicts"));
        }

        // Check profile identity uniqueness
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        List<UserProfileField> identityFields = fieldRepo.findIdentityFields();
        Map<String, Object> profiles = userProfiles != null ? userProfiles : Collections.emptyMap();

        for (UserProfileField f : identityFields) {
            Object value = profiles.get(f.jsonName());
            if (value == null) continue;
            Optional<Long> existingUserId = userRepo.findUserIdByProfileIdentity(f.jsonName(), value.toString());
            if (existingUserId.isPresent()) {
                violations.add(new Problem.Violation(f.jsonName(), "conflicts"));
            }
        }

        if (!violations.isEmpty()) {
            context.setMessage(Problem.valueOf(409));
        }
        return !violations.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Decision(POST)
    public SignUpResponse create(String account,
                                Boolean enablePassword,
                                String code,
                                Map userProfiles,
                                RestContext context,
                                DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        InvitationRepository invitationRepo = new InvitationRepository(dsl);

        // Create user
        User user = userRepo.insert(account);
        context.put(USER_KEY, user);

        // Set profile values and track which need verification
        Map<String, Object> profiles = userProfiles != null ? userProfiles : Collections.emptyMap();
        List<UserProfileField> verificationFields = new ArrayList<>();

        for (Map.Entry<String, Object> entry : ((Map<String, Object>) profiles).entrySet()) {
            fieldRepo.findByJsonName(entry.getKey()).ifPresent(field -> {
                userRepo.setProfileValue(user.id(), field.id(), entry.getValue().toString());
                if (field.needsVerification()) {
                    verificationFields.add(field);
                }
            });
        }

        // Create profile verifications
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

        // Handle invitation
        Optional<Invitation> invitation = Optional.ofNullable(code)
                .flatMap(invitationRepo::findByCode);

        invitation.ifPresent(invi -> {
            // Add user to invitation groups
            if (invi.groupInvitations() != null) {
                for (GroupInvitation gi : invi.groupInvitations()) {
                    userRepo.addToGroup(user.id(), gi.group().id());
                }
            }

            // Handle OIDC invitations
            if (invi.oidcInvitations() != null) {
                for (OidcInvitation oidcInvitation : invi.oidcInvitations()) {
                    JwtClaim claim = jsonWebToken.decodePayload(oidcInvitation.oidcPayload(), new TypeReference<JwtClaim>() {});
                    userRepo.insertOidcUser(oidcInvitation.id(), user.id(), claim.getSub());
                }
            }

            // Delete invitation
            invitationRepo.delete(invi.code());
        });

        // Initialize password if requested
        InitialPassword initialPassword = null;
        if (enablePassword != null && enablePassword) {
            PasswordCredentialService passwordCredentialService = new PasswordCredentialService(dsl, config);
            initialPassword = passwordCredentialService.initializePassword(user);
            context.put(INITIAL_PASSWORD, initialPassword);
        }

        config.getHookRepo().runHook(HookPoint.AFTER_SIGN_UP, context);

        return builder(new SignUpResponse())
                .set(SignUpResponse::setId, user.id())
                .set(SignUpResponse::setAccount, user.account())
                .set(SignUpResponse::setUserProfiles, (Map<String, Object>) profiles)
                .set(SignUpResponse::setPassword, initialPassword != null ? initialPassword.password() : null)
                .build();
    }
}
