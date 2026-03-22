package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfileField;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.util.*;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class UsersResource {
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<String> ACCOUNT = ContextKey.of("account", String.class);
    static final ContextKey<Map> USER_PROFILES = ContextKey.of("userProfiles", Map.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        JsonNode accountNode = body.get("account");
        if (accountNode == null || accountNode.asText().isBlank()) {
            return Problem.valueOf(400, "account is required", BouncrProblem.MALFORMED.problemUri());
        }
        String account = accountNode.asText();
        // Validate format without a decoder — this resource reads JSON nodes directly.
        // The length guard is explicit here because there is no decoder-level maxLength(100).
        if (account.length() > 100 || !account.matches("^\\w+$")) {
            return Problem.valueOf(400, "account format is invalid", BouncrProblem.MALFORMED.problemUri());
        }
        context.put(ACCOUNT, account);

        // Extract user profile fields dynamically
        Map<String, Object> userProfiles = new HashMap<>();
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        Set<String> knownFields = new HashSet<>(List.of("account", "enable_password_credential", "code"));
        fieldRepo.findAll().forEach(f -> knownFields.add(f.jsonName()));

        for (String fieldName : body.propertyNames()) {
            if (!knownFields.contains(fieldName) || fieldRepo.findByJsonName(fieldName).isPresent()) {
                if (fieldRepo.findByJsonName(fieldName).isPresent()) {
                    userProfiles.put(fieldName, body.get(fieldName).asText());
                }
            }
        }
        context.put(USER_PROFILES, userProfiles);

        config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, userProfiles);
        return null;
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read") || p.hasPermission("any_user:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:create") || p.hasPermission("any_user:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean conflict(RestContext context, DSLContext dsl) {
        String account = context.get(ACCOUNT).orElseThrow();
        UserRepository userRepo = new UserRepository(dsl);
        return !userRepo.isAccountUnique(account);
    }

    @SuppressWarnings("unchecked")
    @Decision(HANDLE_OK)
    public List<User> handleOk(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        String q = params.get("q");
        Long groupId = Optional.ofNullable(params.<String>get("group_id")).map(Long::parseLong).orElse(null);
        int offset = Optional.ofNullable(params.<String>get("offset")).map(Integer::parseInt).orElse(0);
        int limit = Optional.ofNullable(params.<String>get("limit")).map(Integer::parseInt).orElse(10);
        boolean isAdmin = principal.hasPermission("any_user:read");
        return userRepo.search(q, groupId, principal.getId(), isAdmin, offset, limit);
    }

    @SuppressWarnings("unchecked")
    @Decision(POST)
    public User doPost(ActionRecord actionRecord,
                       UserPermissionPrincipal principal,
                       RestContext context,
                       DSLContext dsl) {
        String account = context.get(ACCOUNT).orElseThrow();
        Map<String, Object> profiles = (Map<String, Object>) context.get(USER_PROFILES).orElse(Map.of());

        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);

        User user = userRepo.insert(account);
        context.put(USER, user);
        for (Map.Entry<String, Object> entry : profiles.entrySet()) {
            fieldRepo.findByJsonName(entry.getKey()).ifPresent(field ->
                    userRepo.setProfileValue(user.id(), field.id(), entry.getValue().toString()));
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_CREATE_USER, context);
        config.getHookRepo().runHook(HookPoint.AFTER_CREATE_USER, context);

        actionRecord.setActionType(ActionType.USER_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(account);

        return userRepo.findByIdFull(user.id(), false, false).orElse(user);
    }
}
