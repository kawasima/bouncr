package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.encoder.BouncrJsonEncoders;
import net.unit8.bouncr.api.logging.ActionRecord;
import net.unit8.bouncr.api.repository.ApplicationRepository;
import net.unit8.bouncr.api.repository.RealmRepository;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.data.Application;
import net.unit8.bouncr.data.Realm;
import net.unit8.bouncr.data.RealmSpec;
import net.unit8.bouncr.data.WordName;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class RealmResource {
    static final ContextKey<RealmSpec> REALM_SPEC = ContextKey.of(RealmSpec.class);
    static final ContextKey<Application> APPLICATION = ContextKey.of(Application.class);
    static final ContextKey<Realm> REALM = ContextKey.of(Realm.class);

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.REALM_SPEC.decode(body)) {
            case Ok(var spec) -> {
                context.put(REALM_SPEC, spec);
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
                .filter(p -> p.hasPermission("realm:read") || p.hasPermission("any_realm:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:update") || p.hasPermission("any_realm:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("realm:delete") || p.hasPermission("any_realm:delete"))
                .isPresent();
    }

    @Decision(PROCESSABLE)
    public boolean isProcessable(Parameters params, RestContext context, DSLContext dsl) {
        ApplicationRepository appRepo = new ApplicationRepository(dsl);
        Optional<Application> application = appRepo.findByName(new WordName(params.get("name")), false);
        application.ifPresent(a -> context.put(APPLICATION, a));
        return application.isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict() {
        return false;
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, Application application, RestContext context, DSLContext dsl) {
        RealmRepository repo = new RealmRepository(dsl);
        Optional<Realm> realm = repo.findByApplicationAndName(application.name(), params.get("realmName"));
        realm.ifPresent(r -> context.put(REALM, r));
        return realm.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> find(Realm realm) {
        return BouncrJsonEncoders.REALM.encode(realm);
    }

    @Decision(PUT)
    public Map<String, Object> update(RealmSpec realmSpec, Realm realm, Application application, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        RealmRepository repo = new RealmRepository(dsl);
        repo.update(application.id(), realm.name(), realmSpec);
        actionRecord.setActionType(ActionType.REALM_MODIFIED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(realm.name().value());
        return BouncrJsonEncoders.REALM.encode(repo.findByApplicationAndName(application.name(), realmSpec.name().value()).orElseThrow());
    }

    @Decision(DELETE)
    public Void delete(Realm realm, Application application, ActionRecord actionRecord, UserPermissionPrincipal principal, DSLContext dsl) {
        RealmRepository repo = new RealmRepository(dsl);
        repo.delete(application.id(), realm.name());
        actionRecord.setActionType(ActionType.REALM_DELETED);
        actionRecord.setActor(principal.getName());
        actionRecord.setDescription(realm.name().value());
        return null;
    }
}
