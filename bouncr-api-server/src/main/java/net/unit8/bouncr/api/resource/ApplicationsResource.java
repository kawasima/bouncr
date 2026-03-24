package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.util.PaginationParams;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple5;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class ApplicationsResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple5<WordName, String, String, String, String>> CREATE_REQ =
            (ContextKey<Tuple5<WordName, String, String, String, String>>) (ContextKey<?>) ContextKey.of(Tuple5.class);

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.APPLICATION_CREATE.decode(body)) {
            case Ok(Tuple5(var name, var desc, var vp, var pt, var tp)) -> {
                context.put(CREATE_REQ, new Tuple5<>((WordName) name, (String) desc, (String) vp, (String) pt, (String) tp));
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
                .filter(p -> p.hasPermission("application:read") || p.hasPermission("any_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("any_application:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(Tuple5<WordName, String, String, String, String> createRequest, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return !repo.isNameUnique(createRequest._1().value());
    }

    @Decision(HANDLE_OK)
    public List<Application> list(Parameters params, UserPermissionPrincipal principal, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        boolean embedRealms = Objects.equals(params.get("embed"), "realms");
        return repo.search(q, embedRealms, offset, limit);
    }

    @Decision(POST)
    public Application create(Tuple5<WordName, String, String, String, String> createRequest, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return repo.insert(createRequest._1().value(), createRequest._2(),
                createRequest._3(), createRequest._4(), createRequest._5());
    }
}
