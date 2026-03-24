package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.util.PaginationParams;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.UserProfile;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple2;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.util.*;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.raoh.json.JsonDecoders.combine;

@AllowedMethods({"GET", "POST"})
public class UsersResource {
    static final ContextKey<User> USER = ContextKey.of(User.class);
    static final ContextKey<WordName> CREATE_REQ = ContextKey.of(WordName.class);
    static final ContextKey<UserProfile> USER_PROFILE = ContextKey.of(UserProfile.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context, DSLContext dsl) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }

        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);
        var result = combine(BouncrJsonDecoders.USER_CREATE, BouncrJsonDecoders.userProfile(fieldRepo))
                .map(Tuple2::new)
                .decode(body);

        return switch (result) {
            case Ok(Tuple2(WordName createReq, UserProfile profile)) -> {
                config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, profile.values());
                context.put(CREATE_REQ, createReq);
                context.put(USER_PROFILE, profile);
                yield null;
            }
            case Ok<?> _ -> throw new IllegalStateException("Unexpected Ok value");
            case Err(var issues) -> toProblem(issues);
        };
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
    public boolean conflict(WordName createReq, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        return !userRepo.isAccountUnique(createReq.value());
    }

    @Decision(HANDLE_OK)
    public List<User> handleOk(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        String q = params.get("q");
        Long groupId = PaginationParams.parseLong(params.get("group_id"));
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        boolean isAdmin = principal.hasPermission("any_user:read");
        return userRepo.search(q, groupId, principal.getId(), isAdmin, offset, limit);
    }

    @Decision(POST)
    public User doPost(WordName createReq,
                       UserProfile userProfile,
                       ActionRecord actionRecord,
                       UserPermissionPrincipal principal,
                       RestContext context,
                       DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        UserProfileFieldRepository fieldRepo = new UserProfileFieldRepository(dsl);

        User user = userRepo.insert(createReq.value());
        context.put(USER, user);
        for (Map.Entry<String, String> entry : userProfile.values().entrySet()) {
            fieldRepo.findByJsonName(entry.getKey()).ifPresent(field ->
                    userRepo.setProfileValue(user.id(), field.id(), entry.getValue()));
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_CREATE_USER, context);
        config.getHookRepo().runHook(HookPoint.AFTER_CREATE_USER, context);

        actionRecord.setActionType(ActionType.USER_CREATED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(createReq.value());

        return userRepo.findByIdFull(user.id(), false, false).orElse(user);
    }
}
