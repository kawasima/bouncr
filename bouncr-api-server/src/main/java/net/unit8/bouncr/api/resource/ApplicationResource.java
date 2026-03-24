package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.combinator.Tuple5;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class ApplicationResource {
    @SuppressWarnings("unchecked")
    static final ContextKey<Tuple5<WordName, String, String, String, String>> UPDATE_REQ =
            (ContextKey<Tuple5<WordName, String, String, String, String>>) (ContextKey<?>) ContextKey.of(Tuple5.class);
    static final ContextKey<Application> APPLICATION = ContextKey.of(Application.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.APPLICATION_UPDATE.decode(body)) {
            case Ok(Tuple5(var name, var desc, var vp, var pt, var tp)) -> {
                context.put(UPDATE_REQ, new Tuple5<>((WordName) name, (String) desc, (String) vp, (String) pt, (String) tp));
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

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("application:update") || p.hasPermission("any_application:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("application:delete") || p.hasPermission("any_application:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(Tuple5<WordName, String, String, String, String> updateRequest, Parameters params, DSLContext dsl) {
        if (Objects.equals(updateRequest._1().value(), params.get("name"))) {
            return false;
        }
        ApplicationRepository repo = new ApplicationRepository(dsl);
        return !repo.isNameUnique(updateRequest._1().value());
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        boolean embedRealms = Objects.equals(params.get("embed"), "realms");
        Optional<Application> application = repo.findByName(params.get("name"), embedRealms);
        application.ifPresent(a -> context.put(APPLICATION, a));
        return application.isPresent();
    }

    @Decision(HANDLE_OK)
    public Application find(Application application) {
        return application;
    }

    @Decision(PUT)
    public Application update(Tuple5<WordName, String, String, String, String> updateRequest, Application application, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        repo.update(application.name(), updateRequest._1().value(), updateRequest._2(),
                updateRequest._3(), updateRequest._4(), updateRequest._5());
        return repo.findByName(updateRequest._1().value(), false).orElseThrow();
    }

    @Decision(DELETE)
    public Void delete(Application application, DSLContext dsl) {
        ApplicationRepository repo = new ApplicationRepository(dsl);
        repo.delete(application.name());
        return null;
    }
}
