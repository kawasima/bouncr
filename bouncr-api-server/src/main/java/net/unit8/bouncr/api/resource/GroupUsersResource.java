package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.repository.GroupRepository;
import net.unit8.bouncr.data.Group;
import net.unit8.bouncr.data.User;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

import net.unit8.bouncr.api.repository.UserRepository;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST", "DELETE"})
public class GroupUsersResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<List<String>> USER_ACCOUNTS =
            (ContextKey<List<String>>) (ContextKey<?>) ContextKey.of("userAccounts", List.class);
    static final ContextKey<Group> GROUP = ContextKey.of(Group.class);

    @Decision(value = MALFORMED, method = {"POST", "DELETE"})
    public Problem validateRequest(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.GROUP_USERS.decode(body)) {
            case Ok<List<String>> ok -> { context.put(USER_ACCOUNTS, ok.value()); yield null; }
            case Err<List<String>>(var issues) -> toProblem(issues);
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

    @Decision(value = ALLOWED, method = {"POST", "DELETE"})
    public boolean isModifyAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:update") || p.hasPermission("any_group:update"))
                .isPresent();
    }

    @Decision(PROCESSABLE)
    public boolean processable(Parameters params, RestContext context, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        Optional<Group> group = repo.findByName(params.get("name"), false);
        group.ifPresent(g -> context.put(GROUP, g));
        return group.isPresent();
    }

    @Decision(HANDLE_OK)
    public List<User> list(Group group, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        Optional<Group> groupWithUsers = repo.findByName(group.name(), true);
        return groupWithUsers.map(Group::users).orElse(List.of());
    }

    @Decision(POST)
    public List<String> create(List<String> userAccounts, Group group, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        List<Long> userIds = findUserIdsByAccounts(dsl, userAccounts);
        for (Long userId : userIds) {
            repo.addUser(group.name(), userId);
        }
        return userAccounts;
    }

    @Decision(DELETE)
    public List<String> delete(List<String> userAccounts, Group group, DSLContext dsl) {
        GroupRepository repo = new GroupRepository(dsl);
        List<Long> userIds = findUserIdsByAccounts(dsl, userAccounts);
        for (Long userId : userIds) {
            repo.removeUser(group.name(), userId);
        }
        return userAccounts;
    }

    private List<Long> findUserIdsByAccounts(DSLContext dsl, List<String> accounts) {
        return new UserRepository(dsl).findIdsByAccounts(accounts);
    }
}
