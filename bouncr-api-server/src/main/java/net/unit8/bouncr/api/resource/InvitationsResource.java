package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.boundary.IdObject;
import net.unit8.bouncr.api.repository.InvitationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.Email;
import net.unit8.bouncr.data.Invitation;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple2;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class InvitationsResource {
    @Inject
    private BouncrConfiguration config;

    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple2<Email, List<IdObject>>> CREATE_REQ =
            (ContextKey<Tuple2<Email, List<IdObject>>>) (ContextKey<?>) ContextKey.of(Tuple2.class);
    static final ContextKey<Invitation> CREATED = ContextKey.of(Invitation.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.INVITATION_CREATE.decode(body)) {
            case Ok(Tuple2(var email, var groups)) -> {
                @SuppressWarnings("unchecked")
                var typedGroups = (List<IdObject>) groups;
                context.put(CREATE_REQ, new Tuple2<>((Email) email, typedGroups));
                yield null;
            }
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
                .filter(p -> p.hasPermission("invitation:create"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("invitation:create"))
                .isPresent();
    }

    @Decision(POST)
    public boolean create(Tuple2<Email, List<IdObject>> createRequest, RestContext context, DSLContext dsl) {
        InvitationRepository repo = new InvitationRepository(dsl);
        String code = RandomUtils.generateRandomString(8, config.getSecureRandom());
        List<Long> groupIds = createRequest._2() != null
                ? createRequest._2().stream()
                    .map(IdObject::id)
                    .toList()
                : List.of();
        Invitation invitation = repo.insert(createRequest._1().value(), code, LocalDateTime.now(), groupIds);
        context.put(CREATED, invitation);
        return true;
    }

    @Decision(HANDLE_CREATED)
    public Invitation handleCreated(Invitation invitation) {
        return invitation;
    }
}
