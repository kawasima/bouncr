package net.unit8.bouncr.api.resource;

import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.InvitationCreate;
import net.unit8.bouncr.api.repository.InvitationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.Invitation;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
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

    static final ContextKey<InvitationCreate> CREATE_REQ = ContextKey.of(InvitationCreate.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.INVITATION_CREATE.decode(body)) {
            case Ok<InvitationCreate> ok -> { context.put(CREATE_REQ, ok.value()); yield null; }
            case Err<InvitationCreate>(var issues) -> toProblem(issues);
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

    @Decision(POST)
    public Invitation create(InvitationCreate createRequest, DSLContext dsl) {
        InvitationRepository repo = new InvitationRepository(dsl);
        String code = RandomUtils.generateRandomString(8, config.getSecureRandom());
        List<Long> groupIds = createRequest.groups() != null
                ? createRequest.groups().stream()
                    .map(BouncrJsonDecoders.IdObject::id)
                    .toList()
                : List.of();
        return repo.insert(createRequest.email(), code, LocalDateTime.now(), groupIds);
    }
}
