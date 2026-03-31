package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.*;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class UserResource {
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<UserProfile> USER_PROFILE = ContextKey.of(UserProfile.class);
    static final ContextKey<VerificationTargetSet> VERIFICATIONS = ContextKey.of(VerificationTargetSet.class);

    @Inject
    private BouncrConfiguration config;

    @Inject
    private StoreProvider storeProvider;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }

        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        var result = BouncrJsonDecoders.userProfile(fieldRepo).decode(body);

        return switch (result) {
            case Ok<UserProfile> ok -> {
                config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, ok.value().values());
                context.put(USER_PROFILE, ok.value());
                yield null;
            }
            case Err<UserProfile>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read")
                        || p.hasPermission("any_user:read")
                        || (p.hasPermission("my:read") && Objects.equals(p.getName(), params.get("account"))))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:update")
                        || p.hasPermission("any_user:update")
                        || (p.hasPermission("my:update") && Objects.equals(p.getName(), params.get("account"))))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:delete")
                        || p.hasPermission("any_user:delete")
                        || (p.hasPermission("my:delete") && Objects.equals(p.getName(), params.get("account"))))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        String account = params.get("account");

        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());

        boolean embedGroups = embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"));
        boolean embedPermissions = embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("permissions"));
        boolean embedOidcProviders = embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("oidc_providers"));

        Optional<User> maybeUser = userRepo.findByAccount(account);
        if (maybeUser.isEmpty()) return false;

        User basicUser = maybeUser.get();
        User user = userRepo.findByIdFull(basicUser.id(), embedGroups, embedPermissions).orElse(null);
        if (user == null) return false;

        UserRepository userRepo2 = new UserRepository(dsl);
        if (embedOidcProviders) {
            List<OidcUser> oidcUsers = userRepo2.loadOidcUsers(user.id());
            user = new User(user.id(), user.account(), user.writeProtected(),
                    user.groups(), user.userProfileValues(), user.userLock(),
                    oidcUsers, user.permissions(), user.unverifiedProfiles());
        }

        List<String> unverifiedProfiles = userRepo2.loadUnverifiedProfiles(user.id());
        if (!unverifiedProfiles.isEmpty()) {
            user = new User(user.id(), user.account(), user.writeProtected(),
                    user.groups(), user.userProfileValues(), user.userLock(),
                    user.oidcUsers(), user.permissions(), unverifiedProfiles);
        }

        context.put(USER, user);
        return true;
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean conflict(User user, UserProfile userProfile, RestContext context, DSLContext dsl) {
        if (userProfile.values().isEmpty()) return false;

        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        UserRepository userRepo = new UserRepository(dsl);
        List<UserProfileField> identityFields = fieldRepo.findIdentityFields();

        for (UserProfileField f : identityFields) {
            userProfile.get(f.jsonName()).ifPresent(value -> {
                if (userRepo.isProfileIdentityConflict(f.jsonName(), value, user.id())) {
                    context.setMessage(Problem.valueOf(409));
                }
            });
        }
        return context.getMessage().isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> handleOk(User user, Parameters params, DSLContext dsl) {
        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());

        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("permissions"))) {
            UserRepository userRepo = new UserRepository(dsl);
            Set<String> permissions = userRepo.getPermissionsByRealm(user.id()).values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            user = new User(user.id(), user.account(), user.writeProtected(),
                    user.groups(), user.userProfileValues(), user.userLock(),
                    user.oidcUsers(), new ArrayList<>(permissions), user.unverifiedProfiles());
        }

        return BouncrJsonEncoders.encodeUser(user);
    }

    @Decision(PUT)
    public Map<String, Object> update(User user,
                       UserProfile userProfile,
                       ActionRecord actionRecord,
                       UserPermissionPrincipal principal,
                       RestContext context,
                       DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);

        config.getHookRepo().runHook(HookPoint.BEFORE_UPDATE_USER, context);

        List<UserProfileValue> currentValues = userRepo.loadProfileValues(user.id());
        List<UserProfileField> updatedFields = new ArrayList<>();

        for (Map.Entry<String, String> entry : userProfile.values().entrySet()) {
            Optional<UserProfileField> maybeField = fieldRepo.findByJsonName(entry.getKey());
            if (maybeField.isEmpty()) continue;
            UserProfileField profileField = maybeField.get();

            String newValue = entry.getValue();
            Optional<UserProfileValue> existingValue = currentValues.stream()
                    .filter(v -> v.userProfileField().id().equals(profileField.id()))
                    .findAny();

            if (existingValue.isPresent()) {
                if (!Objects.equals(existingValue.get().value(), newValue)) {
                    userRepo.setProfileValue(user.id(), profileField.id(), newValue);
                    updatedFields.add(profileField);
                }
            } else {
                if (newValue != null) {
                    userRepo.setProfileValue(user.id(), profileField.id(), newValue);
                    updatedFields.add(profileField);
                }
            }
        }

        List<UserProfileVerification> newVerifications = updatedFields.stream()
                .filter(UserProfileField::needsVerification)
                .map(f -> {
                    String code = net.unit8.bouncr.util.RandomUtils.generateRandomString(6);
                    java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusDays(1);
                    userRepo.upsertProfileVerification(user.id(), f.id(), code, expiresAt);
                    return new UserProfileVerification(
                            new UserProfileVerificationId(f.id(), user.id()),
                            code, expiresAt);
                })
                .collect(Collectors.toList());

        if (!newVerifications.isEmpty()) {
            context.put(VERIFICATIONS, new VerificationTargetSet(newVerifications));
        }

        config.getHookRepo().runHook(HookPoint.AFTER_UPDATE_USER, context);

        actionRecord.setActionType(ActionType.USER_MODIFIED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(user.account());

        return BouncrJsonEncoders.encodeUser(
                userRepo.findByIdFull(user.id(), false, false).orElse(user));
    }

    @Decision(DELETE)
    public Void delete(User user,
                       ActionRecord actionRecord,
                       UserPermissionPrincipal principal,
                       RestContext context,
                       DSLContext dsl) {
        config.getHookRepo().runHook(HookPoint.BEFORE_DELETE_USER, context);

        UserRepository userRepo = new UserRepository(dsl);
        userRepo.deleteProfileVerifications(user.id());
        userRepo.delete(user.id());

        config.getHookRepo().runHook(HookPoint.AFTER_DELETE_USER, context);

        actionRecord.setActionType(ActionType.USER_DELETED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(user.account());

        return null;
    }

}
